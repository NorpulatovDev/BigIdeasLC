package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreatePaymentRequest {
    @NotNull(message = "O'quvchi majburiy")
    private Long studentId;

    @NotNull(message = "Guruh majburiy")
    private Long groupId;

    @NotNull(message = "Miqdor majburiy")
    @DecimalMin(value = "0.0", inclusive = false, message = "Miqdor 0 dan katta bo'lishi kerak")
    private BigDecimal amount;

    @Size(max = 255, message = "Tavsif 255 harfdan kam bo'lishi kerak")
    private String description;

    @NotNull(message = "To'lov turi majburiy")
    @Pattern(regexp = "^(CARD|CASH)$", message = "To'lov turi CARD yoki CASH bo'lishi kerak")
    private String category;

    @NotNull(message = "Filial majburiy")
    private Long branchId;

    @NotNull(message = "To'lov sanasi majburiy")
    private LocalDate paymentDate;

    @NotNull(message = "To'lov muddati majburiy")
    private LocalDate paymentDueDate;
}
