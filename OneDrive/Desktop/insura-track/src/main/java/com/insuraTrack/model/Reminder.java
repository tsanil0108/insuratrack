package com.insuraTrack.model;

import com.insuraTrack.enums.ReminderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "reminders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    private LocalDate reminderDate;

    @Enumerated(EnumType.STRING)
    private ReminderType type;

    private String message;

    private String severity; // HIGH, MEDIUM, LOW

    private boolean sent = false;

    private boolean dismissed = false;
}