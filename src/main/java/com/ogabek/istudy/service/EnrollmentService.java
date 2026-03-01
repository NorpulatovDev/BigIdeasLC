package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.EnrollmentRequest;
import com.ogabek.istudy.dto.response.EnrollmentDto;
import com.ogabek.istudy.entity.Enrollment;
import com.ogabek.istudy.entity.Group;
import com.ogabek.istudy.entity.Student;
import com.ogabek.istudy.repository.EnrollmentRepository;
import com.ogabek.istudy.repository.GroupRepository;
import com.ogabek.istudy.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public EnrollmentDto createEnrollment(EnrollmentRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found: " + request.getStudentId()));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found: " + request.getGroupId()));

        if (!student.getBranch().getId().equals(group.getBranch().getId())) {
            throw new RuntimeException("O'quvchi va guruh bir filialda bo'lishi kerak!");
        }

        if (enrollmentRepository.findByStudentIdAndGroupId(request.getStudentId(), request.getGroupId()).isPresent()) {
            throw new RuntimeException("O'quvchi bu guruhda allaqachon ro'yxatda!");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setGroup(group);
        enrollment.setTuitionFee(request.getTuitionFee());
        enrollment.setTeacherSalaryAmount(request.getTeacherSalaryAmount());

        Enrollment saved = enrollmentRepository.save(enrollment);
        return convertToDto(saved);
    }

    @Transactional
    public EnrollmentDto updateEnrollment(Long id, EnrollmentRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + id));

        if (request.getTuitionFee() != null) {
            enrollment.setTuitionFee(request.getTuitionFee());
        }
        if (request.getTeacherSalaryAmount() != null) {
            enrollment.setTeacherSalaryAmount(request.getTeacherSalaryAmount());
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        return convertToDto(saved);
    }

    @Transactional
    public void deleteEnrollment(Long id) {
        if (!enrollmentRepository.existsById(id)) {
            throw new RuntimeException("Enrollment not found: " + id);
        }
        enrollmentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> getEnrollments(Long studentId, Long groupId) {
        List<Enrollment> enrollments;
        if (studentId != null && groupId != null) {
            enrollments = enrollmentRepository.findByStudentIdAndGroupId(studentId, groupId)
                    .map(List::of)
                    .orElse(List.of());
        } else if (studentId != null) {
            enrollments = enrollmentRepository.findByStudentId(studentId);
        } else if (groupId != null) {
            enrollments = enrollmentRepository.findByGroupId(groupId);
        } else {
            throw new RuntimeException("studentId or groupId must be provided");
        }
        return enrollments.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private EnrollmentDto convertToDto(Enrollment enrollment) {
        EnrollmentDto dto = new EnrollmentDto();
        dto.setId(enrollment.getId());
        if (enrollment.getStudent() != null) {
            dto.setStudentId(enrollment.getStudent().getId());
            dto.setStudentName(enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName());
        }
        if (enrollment.getGroup() != null) {
            dto.setGroupId(enrollment.getGroup().getId());
            dto.setGroupName(enrollment.getGroup().getName());
        }
        dto.setTuitionFee(enrollment.getTuitionFee());
        dto.setTeacherSalaryAmount(enrollment.getTeacherSalaryAmount());
        dto.setEnrolledAt(enrollment.getEnrolledAt());
        return dto;
    }
}
