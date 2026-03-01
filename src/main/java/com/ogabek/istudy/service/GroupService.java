package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateGroupRequest;
import com.ogabek.istudy.dto.response.GroupDto;
import com.ogabek.istudy.dto.response.StudentDto;
import com.ogabek.istudy.dto.response.StudentPaymentInfo;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final BranchRepository branchRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<GroupDto> getGroupsByBranch(Long branchId) {
        return groupRepository.findByBranchIdWithAllRelations(branchId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getUnpaidStudentsByGroup(Long groupId, Integer year, Integer month) {
        Group group = groupRepository.findByIdWithAllRelations(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        List<Student> unpaidStudents = studentRepository.findUnpaidStudentsByBranchAndMonth(
                group.getBranch().getId(), targetYear, targetMonth);

        Set<Long> unpaidStudentIds = unpaidStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toSet());

        return enrollmentRepository.findByGroupId(groupId).stream()
                .filter(e -> unpaidStudentIds.contains(e.getStudent().getId()))
                .map(e -> convertStudentToDto(e.getStudent(), targetYear, targetMonth))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupDto getGroupById(Long id) {
        Group group = groupRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));
        return convertToDto(group);
    }

    @Transactional(readOnly = true)
    public GroupDto getGroupById(Long id, Integer year, Integer month) {
        Group group = groupRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));
        return convertToDtoWithStudentPayments(group, year, month);
    }

    @Transactional
    public GroupDto createGroup(CreateGroupRequest request) {
        Teacher teacher = teacherRepository.findByIdWithBranch(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + request.getTeacherId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setTeacher(teacher);
        group.setBranch(branch);
        group.setStartTime(request.getStartTime());
        group.setEndTime(request.getEndTime());

        if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            group.setDaysOfWeek(String.join(",", request.getDaysOfWeek()));
        }

        Group savedGroup = groupRepository.save(group);

        Group groupWithRelations = groupRepository.findByIdWithAllRelations(savedGroup.getId())
                .orElseThrow(() -> new RuntimeException("Failed to fetch created group"));

        return convertToDto(groupWithRelations);
    }

    @Transactional
    public GroupDto updateGroup(Long id, CreateGroupRequest request) {
        Group group = groupRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));

        Teacher teacher = teacherRepository.findByIdWithBranch(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + request.getTeacherId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setTeacher(teacher);
        group.setBranch(branch);
        group.setStartTime(request.getStartTime());
        group.setEndTime(request.getEndTime());

        if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            group.setDaysOfWeek(String.join(",", request.getDaysOfWeek()));
        }

        Group savedGroup = groupRepository.save(group);

        Group groupWithRelations = groupRepository.findByIdWithAllRelations(savedGroup.getId())
                .orElseThrow(() -> new RuntimeException("Failed to fetch updated group"));

        return convertToDto(groupWithRelations);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + id));

        group.setDeleted(true);
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getGroupStudents(Long groupId, Integer year, Integer month) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        return enrollmentRepository.findByGroupId(groupId).stream()
                .map(e -> convertStudentToDto(e.getStudent(), targetYear, targetMonth))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getGroupsByTeacher(Long teacherId, int year, int month) {
        return groupRepository.findByTeacherIdWithRelations(teacherId).stream()
                .map(group -> convertToDtoWithStudentPayments(group, year, month))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getGroupsByTeacher(Long teacherId) {
        LocalDate now = LocalDate.now();
        return getGroupsByTeacher(teacherId, now.getYear(), now.getMonthValue());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> searchGroupsByName(Long branchId, String name) {
        return groupRepository.findByBranchIdAndNameContainingIgnoreCase(branchId, name).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private GroupDto convertToDto(Group group) {
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

    private GroupDto convertToDtoWithStudentPayments(Group group, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        GroupDto dto = convertToDto(group);
        calculateStudentPayments(dto, group, targetYear, targetMonth);
        return dto;
    }

    private void calculateStudentPayments(GroupDto dto, Group group, int year, int month) {
        List<StudentPaymentInfo> studentPayments = new ArrayList<>();

        List<Enrollment> enrollments = enrollmentRepository.findByGroupId(group.getId());

        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            BigDecimal tuitionFee = enrollment.getTuitionFee() != null ? enrollment.getTuitionFee() : BigDecimal.ZERO;

            BigDecimal studentTotalPaid = paymentRepository.getTotalPaidByStudentInGroupForMonth(
                    student.getId(), group.getId(), year, month);
            studentTotalPaid = studentTotalPaid != null ? studentTotalPaid : BigDecimal.ZERO;

            BigDecimal remainingAmount = tuitionFee.subtract(studentTotalPaid);
            remainingAmount = remainingAmount.compareTo(BigDecimal.ZERO) > 0 ? remainingAmount : BigDecimal.ZERO;

            String paymentStatus;
            if (studentTotalPaid.compareTo(BigDecimal.ZERO) == 0) {
                paymentStatus = "UNPAID";
            } else if (studentTotalPaid.compareTo(tuitionFee) >= 0) {
                paymentStatus = "PAID";
            } else {
                paymentStatus = "PARTIAL";
            }

            studentPayments.add(new StudentPaymentInfo(
                    student.getId(),
                    student.getFirstName() + " " + student.getLastName(),
                    student.getPhoneNumber(),
                    student.getParentPhoneNumber(),
                    studentTotalPaid,
                    tuitionFee,
                    remainingAmount,
                    paymentStatus,
                    enrollment.getId()
            ));
        }

        dto.setStudentPayments(studentPayments);
    }

    private StudentDto convertStudentToDto(Student student, int year, int month) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setPhoneNumber(student.getPhoneNumber());

        if (student.getBranch() != null) {
            dto.setBranchId(student.getBranch().getId());
            dto.setBranchName(student.getBranch().getName());
        }

        dto.setCreatedAt(student.getCreatedAt());
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

        if (dto.getTotalPaidInMonth().compareTo(BigDecimal.ZERO) == 0) {
            dto.setPaymentStatus("UNPAID");
        } else if (dto.getTotalPaidInMonth().compareTo(expectedPayment) >= 0) {
            dto.setPaymentStatus("PAID");
        } else {
            dto.setPaymentStatus("PARTIAL");
        }

        dto.setLastPaymentDate(studentRepository.getLastPaymentDate(studentId));
    }
}
