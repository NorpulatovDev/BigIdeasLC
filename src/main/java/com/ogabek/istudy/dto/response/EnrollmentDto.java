package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class EnrollmentDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long groupId;
    private String groupName;
    private BigDecimal tuitionFee;
    private BigDecimal teacherSalaryAmount;
    private LocalDate enrolledAt;
}
