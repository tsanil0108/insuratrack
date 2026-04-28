package com.insuraTrack.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRequest {

    private String companyId;
    private String insuranceTypeId;
    private String insuranceItemId;
    private String providerId;
    private String hypothecationId;

    private String policyNumber;
    private String description;

    private double premiumAmount;
    private double sumInsured;

    private LocalDate startDate;
    private LocalDate endDate;

    private String premiumFrequency;

    // ✅ Payment fields (sent by frontend)
    private double amountPaid;
    private LocalDate paidDate;
    private String paymentMode;
    private String paymentReference;
    private boolean paid;
}