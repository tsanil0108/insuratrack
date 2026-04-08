package com.insuraTrack.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecycleBinItem {
    private String id;
    private String type;          // POLICY, COMPANY, PAYMENT, PROVIDER
    private String name;          // display name
    private String policyNumber;
    private String companyName;
    private String description;
    private String deletedBy;
    private String deletedAt;
}