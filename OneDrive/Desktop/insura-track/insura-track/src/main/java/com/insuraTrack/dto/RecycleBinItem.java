package com.insuraTrack.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecycleBinItem {
    private String id;
    private String type;
    private String name;
    private String policyNumber;
    private String assignedTo;
    private String companyName;
    private String providerName;
    private String insuranceTypeName;   // ← NEW
    private String description;
    private String deletedBy;
    private String deletedAt;
}