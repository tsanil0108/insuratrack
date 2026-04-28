package com.insuraTrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
    private String id;
    private String policyNumber;

    // Company fields
    private String companyId;
    private String companyName;

    // Provider fields
    private String providerId;
    private String providerName;

    // Insurance Type fields
    private String insuranceTypeId;
    private String insuranceTypeName;

    // Insurance Item fields
    private String insuranceItemId;
    private String insuranceItemName;

    // Hypothecation fields
    private String hypothecationId;
    private String hypothecationName;
    private boolean hypothecation;  // ✅ Add this field for boolean flag

    // User fields
    private String userId;
    private String userName;
    private String userEmail;

    // Financial fields
    private double premiumAmount;
    private double sumInsured;
    private double amountPaid;

    // Date fields
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate paidDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Status fields
    private String status;
    private String premiumFrequency;
    private String paymentMode;
    private String paymentReference;

    // Other fields
    private String description;
    private boolean paid;
    private boolean deleted;

    // For renewal
    private String renewedFromPolicyId;
    private String renewedToPolicyId;
}