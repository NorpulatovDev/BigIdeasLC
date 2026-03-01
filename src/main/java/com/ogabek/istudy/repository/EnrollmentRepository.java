package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("SELECT e FROM Enrollment e LEFT JOIN FETCH e.student LEFT JOIN FETCH e.group WHERE e.group.id = :groupId")
    List<Enrollment> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT e FROM Enrollment e LEFT JOIN FETCH e.student LEFT JOIN FETCH e.group LEFT JOIN FETCH e.group.teacher WHERE e.student.id = :studentId")
    List<Enrollment> findByStudentId(@Param("studentId") Long studentId);

    Optional<Enrollment> findByStudentIdAndGroupId(Long studentId, Long groupId);

    void deleteByStudentIdAndGroupId(Long studentId, Long groupId);

    void deleteByStudentId(Long studentId);

    long countByGroupId(Long groupId);
}
