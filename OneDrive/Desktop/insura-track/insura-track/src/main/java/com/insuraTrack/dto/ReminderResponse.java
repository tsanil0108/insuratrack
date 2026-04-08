package com.insuraTrack.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ReminderResponse {
    private String id;
    private String policyId;
    private String policyNumber;
    private LocalDate reminderDate;
    private String type;
    private String message;
    private String severity;
    private boolean sent;
    private boolean dismissed;
    private LocalDateTime createdAt;
}