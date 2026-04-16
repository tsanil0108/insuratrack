package com.insuraTrack.model;

import com.insuraTrack.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "premium_payments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    private double amount;

    private LocalDate dueDate;

    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String remarks;

    // New fields for payment slip
    @Column(length = 500)
    private String paymentSlipUrl;  // URL/path to uploaded payment slip

    private String paymentReference; // Transaction/Reference number

    private String paymentMethod;    // UPI, Bank Transfer, Credit Card, etc.

    private LocalDate paymentVerifiedDate; // When admin verified the payment

    @Column(length = 500)
    private String adminRemarks; // Admin remarks on payment verification
}