package com.insuraTrack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.insuraTrack.enums.PaymentMode;
import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.enums.PremiumFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_type_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InsuranceType insuranceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_item_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InsuranceItem insuranceItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InsuranceProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothecation_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Hypothecation hypothecation;

    @Column(unique = true, nullable = false)
    private String policyNumber;

    private String description;
    private double premiumAmount;
    private double sumInsured;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PremiumFrequency premiumFrequency;

    private double amountPaid;
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String paymentReference;
    private String paymentSlipPath;
    private boolean paid = false;

    // ✅ ADD THIS FIELD FOR RENEWAL TRACKING
    @Column(name = "renewed_from_policy_id")
    private String renewedFromPolicyId;

    @Transient
    public double getRemainingAmount() {
        return Math.max(0, premiumAmount - amountPaid);
    }

    @Transient
    public boolean isFullyPaid() {
        return amountPaid >= premiumAmount;
    }
}