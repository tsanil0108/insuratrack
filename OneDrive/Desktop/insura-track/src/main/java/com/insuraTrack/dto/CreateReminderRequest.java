package com.insuraTrack.dto;

import com.insuraTrack.enums.ReminderType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateReminderRequest {
    private String policyId;
    private LocalDate reminderDate;
    private ReminderType type;
    private String message;
    private String severity; // HIGH, MEDIUM, LOW
}