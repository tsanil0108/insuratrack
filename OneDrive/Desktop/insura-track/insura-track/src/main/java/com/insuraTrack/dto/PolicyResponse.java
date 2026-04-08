package com.insuraTrack.dto;

import com.insuraTrack.enums.PolicyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Builder
public class PolicyResponse {
    private String id;
    private String policyNumber;
    private String companyId;
    private String companyName;
    private String insuranceTypeId;
    private String insuranceTypeName;
    private String providerId;
    private String providerName;
    private String userId;         // ← added
    private String userName;       // ← added
    private String description;
    private double premiumAmount;
    private double sumInsured;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String premiumFrequency;
    private boolean hypothecation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}