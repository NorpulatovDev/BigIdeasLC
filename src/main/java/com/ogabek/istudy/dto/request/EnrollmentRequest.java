package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EnrollmentRequest {
    @NotNull(message = "O'quvchi majburiy")
    private Long studentId;

    @NotNull(message = "Guruh majburiy")
    private Long groupId;

    @NotNull(message = "To'lov miqdori majburiy")
    @DecimalMin(value = "0.0", inclusive = false, message = "To'lov miqdori 0 dan katta bo'lishi kerak")
    private BigDecimal tuitionFee;

    @DecimalMin(value = "0.0", inclusive = true, message = "O'qituvchi maoshi 0 yoki undan katta bo'lishi kerak")
    private BigDecimal teacherSalaryAmount;
}
