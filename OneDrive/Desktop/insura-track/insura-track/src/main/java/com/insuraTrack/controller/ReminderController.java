package com.insuraTrack.controller;

import com.insuraTrack.dto.ReminderResponse;
import com.insuraTrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public List<ReminderResponse> getActiveReminders() {
        return reminderService.getActiveReminders();
    }

    @GetMapping("/policy/{policyId}")
    public List<ReminderResponse> getByPolicy(@PathVariable String policyId) {
        return reminderService.getRemindersByPolicy(policyId);
    }

    @PutMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReminderResponse> dismiss(@PathVariable String id) {
        return ResponseEntity.ok(reminderService.dismissReminder(id));
    }

    @PutMapping("/policy/{policyId}/dismiss-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> dismissAllByPolicy(@PathVariable String policyId) {
        reminderService.dismissAllByPolicy(policyId);
        return ResponseEntity.ok().build();
    }
}