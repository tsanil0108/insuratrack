package com.insuraTrack.dto;

import com.insuraTrack.enums.PremiumFrequency;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PolicyRequest {
    private String policyNumber;
    private String companyId;
    private String insuranceTypeId;
    private String providerId;
    private String userId;        // ← added for user assignment
    private String description;
    private double premiumAmount;
    private double sumInsured;
    private LocalDate startDate;
    private LocalDate endDate;
    private PremiumFrequency premiumFrequency;
    private boolean hypothecation;
}