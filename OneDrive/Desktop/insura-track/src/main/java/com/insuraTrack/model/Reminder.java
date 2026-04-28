package com.insuraTrack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.insuraTrack.enums.ReminderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Policy policy;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReminderType type;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "severity")
    private String severity;

    @Column(name = "sent")
    private boolean sent = false;

    @Column(name = "dismissed")
    private boolean dismissed = false;
}