package com.ogabek.istudy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "group_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tuitionFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal teacherSalaryAmount;

    @Column(nullable = false)
    private LocalDate enrolledAt;

    @PrePersist
    protected void onPrePersist() {
        if (enrolledAt == null) {
            enrolledAt = LocalDate.now();
        }
    }
}
