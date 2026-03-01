// src/main/java/com/ogabek/istudy/service/StudentService.java
package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateStudentRequest;
import com.ogabek.istudy.dto.response.GroupDto;
import com.ogabek.istudy.dto.response.PaymentDto;
import com.ogabek.istudy.dto.response.StudentDto;
import com.ogabek.istudy.dto.response.UnpaidStudentDto;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final BranchRepository branchRepository;
    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByBranch(Long branchId) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return studentRepository.findByBranchIdWithBranch(branchId).stream()
                .map(student -> convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByBranch(Long branchId, Integer year, Integer month) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();

        return studentRepository.findByBranchIdWithBranch(branchId).stream()
                .map(student -> convertToDto(student, targetYear, targetMonth))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByGroup(Long groupId, Integer year, Integer month) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();

        return enrollmentRepository.findByGroupId(groupId).stream()
                .map(e -> convertToDto(e.getStudent(), targetYear, targetMonth))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UnpaidStudentDto> getUnpaidStudents(Long branchId, Integer year, Integer month) {
        List<UnpaidStudentDto> result = new ArrayList<>();
        List<Group> branchGroups = groupRepository.findByBranchIdWithAllRelations(branchId);

        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();

        for (Group group : branchGroups) {
            List<Enrollment> enrollments = enrollmentRepository.findByGroupId(group.getId());
            for (Enrollment enrollment : enrollments) {
                Student student = enrollment.getStudent();
                BigDecimal tuitionFee = enrollment.getTuitionFee() != null ? enrollment.getTuitionFee() : BigDecimal.ZERO;

                BigDecimal totalPaid;
                if (year == null || month == null) {
                    totalPaid = paymentRepository.findByStudentIdWithRelations(student.getId())
                            .stream()
                            .filter(payment -> payment.getGroup().getId().equals(group.getId()))
                            .map(Payment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                } else {
                    totalPaid = paymentRepository.getTotalPaidByStudentInGroupForMonth(
                            student.getId(), group.getId(), targetYear, targetMonth);
                    totalPaid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
                }

                BigDecimal remainingAmount = tuitionFee.subtract(totalPaid);
                boolean isOverdue = isPaymentOverdue(student, targetYear, targetMonth);

                if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && isOverdue) {
                    result.add(new UnpaidStudentDto(
                            student.getId(),
                            student.getFirstName(),
                            student.getLastName(),
                            student.getPhoneNumber(),
                            student.getParentPhoneNumber(),
                            remainingAmount,
                            group.getId(),
                            group.getName()));
                }
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> searchStudentsByName(Long branchId, String name) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return studentRepository.findByBranchIdAndFullName(branchId, name).stream()
                .map(student -> convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getStudentPaymentHistory(Long studentId) {
        return paymentRepository.findByStudentIdWithRelations(studentId).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getStudentGroups(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(e -> convertGroupToDto(e.getGroup()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentStatistics(Long branchId) {
        List<Student> allStudents = studentRepository.findByBranchId(branchId);
        LocalDate paymentPeriod = getCurrentPaymentPeriod();

        long paidCount = 0;
        long unpaidCount = 0;
        long upcomingCount = 0;
        long overdueCount = 0;

        for (Student student : allStudents) {
            StudentDto dto = convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue());
            String status = dto.getPaymentStatus();

            switch (status) {
                case "PAID":
                    paidCount++;
                    break;
                case "OVERDUE":
                    overdueCount++;
                    break;
                case "UPCOMING":
                    upcomingCount++;
                    break;
                default:
                    unpaidCount++;
                    break;
            }
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalStudents", allStudents.size());
        statistics.put("paidStudents", paidCount);
        statistics.put("unpaidStudents", unpaidCount);
        statistics.put("upcomingStudents", upcomingCount);
        statistics.put("overdueStudents", overdueCount);
        statistics.put("paymentRate",
                allStudents.size() > 0
                        ? (double) paidCount / allStudents.size() * 100
                        : 0);

        return statistics;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getRecentStudents(Long branchId, int limit) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return studentRepository.findByBranchId(branchId).stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(limit)
                .map(student -> convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue());
    }

    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id, Integer year, Integer month) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();
        return convertToDto(student, targetYear, targetMonth);
    }

    @Transactional
    public StudentDto createStudent(CreateStudentRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        Student student = new Student();
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setParentPhoneNumber(request.getParentPhoneNumber());
        student.setBranch(branch);
        student.setPaymentDayOfMonth(request.getPaymentDayOfMonth());

        Student savedStudent = studentRepository.save(student);

        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return convertToDto(savedStudent, paymentPeriod.getYear(), paymentPeriod.getMonthValue());
    }

    @Transactional
    public StudentDto updateStudent(Long id, CreateStudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setParentPhoneNumber(request.getParentPhoneNumber());
        student.setBranch(branch);
        student.setPaymentDayOfMonth(request.getPaymentDayOfMonth());

        Student savedStudent = studentRepository.save(student);

        LocalDate now = LocalDate.now();
        return convertToDto(savedStudent, now.getYear(), now.getMonthValue());
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("O'quvchi topilmadi: " + id));

        student.setDeleted(true);
        studentRepository.save(student);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private LocalDate getCurrentPaymentPeriod() {
        LocalDate now = LocalDate.now();
        if (now.getDayOfMonth() < 5) {
            return now.minusMonths(1);
        }
        return now;
    }

    private LocalDate calculateNextDueDate(Student student, int year, int month) {
        if (student.getPaymentDayOfMonth() == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        int dayOfMonth = student.getPaymentDayOfMonth();

        try {
            LocalDate dueDate = LocalDate.of(year, month, dayOfMonth);
            if (dueDate.isBefore(today)) {
                LocalDate nextMonth = dueDate.plusMonths(1);
                try {
                    return LocalDate.of(nextMonth.getYear(), nextMonth.getMonthValue(), dayOfMonth);
                } catch (Exception e) {
                    return nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
                }
            }
            return dueDate;
        } catch (Exception e) {
            LocalDate firstOfMonth = LocalDate.of(year, month, 1);
            return firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
        }
    }

    private boolean isPaymentOverdue(Student student, int year, int month) {
        if (student.getPaymentDayOfMonth() == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate;

        try {
            dueDate = LocalDate.of(year, month, student.getPaymentDayOfMonth());
        } catch (Exception e) {
            dueDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        }

        return today.isAfter(dueDate);
    }

    private StudentDto convertToDto(Student student, int year, int month) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setPhoneNumber(student.getPhoneNumber());
        dto.setParentPhoneNumber(student.getParentPhoneNumber());
        dto.setPaymentDayOfMonth(student.getPaymentDayOfMonth());

        if (student.getBranch() != null) {
            dto.setBranchId(student.getBranch().getId());
            dto.setBranchName(student.getBranch().getName());
        }

        dto.setCreatedAt(student.getCreatedAt());

        LocalDate nextDueDate = calculateNextDueDate(student, year, month);
        dto.setNextDueDate(nextDueDate);

        List<Enrollment> studentEnrollments = enrollmentRepository.findByStudentId(student.getId());
        List<StudentDto.GroupInfo> groupInfos = studentEnrollments.stream()
                .map(enrollment -> {
                    Group group = enrollment.getGroup();
                    String teacherName = group.getTeacher() != null
                            ? group.getTeacher().getFirstName() + " " + group.getTeacher().getLastName()
                            : null;
                    return new StudentDto.GroupInfo(
                            group.getId(),
                            group.getName(),
                            enrollment.getTuitionFee(),
                            teacherName,
                            enrollment.getId());
                })
                .collect(Collectors.toList());

        dto.setGroups(groupInfos);

        calculatePaymentStatus(dto, student.getId(), year, month);

        return dto;
    }

    private void calculatePaymentStatus(StudentDto dto, Long studentId, int year, int month) {
        Boolean hasPaid = studentRepository.hasStudentPaidInMonth(studentId, year, month);
        dto.setHasPaidInMonth(hasPaid != null ? hasPaid : false);

        BigDecimal totalPaid = studentRepository.getTotalPaidByStudentInMonth(studentId, year, month);
        dto.setTotalPaidInMonth(totalPaid != null ? totalPaid : BigDecimal.ZERO);

        BigDecimal expectedPayment = studentRepository.getExpectedMonthlyPaymentForStudent(studentId);
        expectedPayment = expectedPayment != null ? expectedPayment : BigDecimal.ZERO;

        BigDecimal remaining = expectedPayment.subtract(dto.getTotalPaidInMonth());
        dto.setRemainingAmount(remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);

        LocalDate nextDueDate = dto.getNextDueDate();
        LocalDate today = LocalDate.now();

        if (dto.getTotalPaidInMonth().compareTo(expectedPayment) >= 0) {
            dto.setPaymentStatus("PAID");
        } else if (nextDueDate == null) {
            if (dto.getTotalPaidInMonth().compareTo(BigDecimal.ZERO) == 0) {
                dto.setPaymentStatus("UNPAID");
            } else {
                dto.setPaymentStatus("PARTIAL");
            }
        } else {
            boolean hasPartialPayment = dto.getTotalPaidInMonth().compareTo(BigDecimal.ZERO) > 0;

            if (today.isBefore(nextDueDate)) {
                dto.setPaymentStatus(hasPartialPayment ? "PARTIAL" : "UPCOMING");
            } else {
                long daysOverdue = ChronoUnit.DAYS.between(nextDueDate, today);
                if (daysOverdue >= 7) {
                    dto.setPaymentStatus("OVERDUE");
                } else {
                    dto.setPaymentStatus(hasPartialPayment ? "PARTIAL" : "UNPAID");
                }
            }
        }

        LocalDateTime lastPaymentDate = studentRepository.getLastPaymentDate(studentId);
        dto.setLastPaymentDate(lastPaymentDate);
    }

    private PaymentDto convertPaymentToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        if (payment.getStudent() != null) {
            dto.setStudentId(payment.getStudent().getId());
            dto.setStudentName(payment.getStudent().getFirstName() + " " + payment.getStudent().getLastName());
        }
        if (payment.getGroup() != null) {
            dto.setGroupId(payment.getGroup().getId());
            dto.setGroupName(payment.getGroup().getName());
        }
        dto.setAmount(payment.getAmount());
        dto.setDescription(payment.getDescription());
        dto.setStatus(payment.getStatus().name());
        if (payment.getBranch() != null) {
            dto.setBranchId(payment.getBranch().getId());
            dto.setBranchName(payment.getBranch().getName());
        }
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentDueDate(payment.getPaymentDueDate());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    private GroupDto convertGroupToDto(Group group) {
        GroupDto dto = new GroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());

        if (group.getTeacher() != null) {
            dto.setTeacherId(group.getTeacher().getId());
            dto.setTeacherName(group.getTeacher().getFirstName() + " " + group.getTeacher().getLastName());
        }

        if (group.getBranch() != null) {
            dto.setBranchId(group.getBranch().getId());
            dto.setBranchName(group.getBranch().getName());
        }

        dto.setStartTime(group.getStartTime());
        dto.setEndTime(group.getEndTime());

        if (group.getDaysOfWeek() != null && !group.getDaysOfWeek().isEmpty()) {
            dto.setDaysOfWeek(Arrays.asList(group.getDaysOfWeek().split(",")));
        } else {
            dto.setDaysOfWeek(new ArrayList<>());
        }

        dto.setStudentCount((int) enrollmentRepository.countByGroupId(group.getId()));
        dto.setCreatedAt(group.getCreatedAt());
        return dto;
    }
}
