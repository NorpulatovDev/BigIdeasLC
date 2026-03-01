package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StudentPaymentInfo {
    private Long studentId;
    private String studentName;
    private String phoneNumber;
    private String parentPhoneNumber;
    private BigDecimal totalPaidInMonth;
    private BigDecimal tuitionFee;
    private BigDecimal remainingAmount;
    private String paymentStatus;
    private Long enrollmentId;

    public StudentPaymentInfo() {}

    public StudentPaymentInfo(Long studentId, String studentName, String phoneNumber,
                              String parentPhoneNumber, BigDecimal totalPaidInMonth,
                              BigDecimal tuitionFee, BigDecimal remainingAmount,
                              String paymentStatus, Long enrollmentId) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.phoneNumber = phoneNumber;
        this.parentPhoneNumber = parentPhoneNumber;
        this.totalPaidInMonth = totalPaidInMonth;
        this.tuitionFee = tuitionFee;
        this.remainingAmount = remainingAmount;
        this.paymentStatus = paymentStatus;
        this.enrollmentId = enrollmentId;
    }
}
