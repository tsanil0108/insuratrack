package com.insuraTrack.dto;

import com.insuraTrack.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PaymentDTO {
    private String id;
    private String policyId;
    private String policyNumber;
    private double amount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private PaymentStatus status;
    private String remarks;

    // New fields
    private String paymentSlipUrl;
    private String paymentReference;
    private String paymentMethod;
    private LocalDate paymentVerifiedDate;
    private String adminRemarks;
}