package com.insuraTrack.controller;

import com.insuraTrack.dto.CreateReminderRequest;
import com.insuraTrack.dto.ReminderResponse;
import com.insuraTrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    // ── ADMIN: all active ──────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReminderResponse>> getActiveReminders() {
        return ResponseEntity.ok(reminderService.getActiveReminders());
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReminderResponse>> getActive() {
        return ResponseEntity.ok(reminderService.getActiveReminders());
    }

    // ── ADMIN: all dismissed ───────────────────────────────
    @GetMapping("/dismissed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReminderResponse>> getDismissed() {
        return ResponseEntity.ok(reminderService.getDismissedReminders());
    }

    // ── USER: own active ───────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<List<ReminderResponse>> getMyReminders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                reminderService.getActiveRemindersByUsername(userDetails.getUsername()));
    }

    @GetMapping("/my/active")
    public ResponseEntity<List<ReminderResponse>> getMyActive(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                reminderService.getActiveRemindersByUsername(userDetails.getUsername()));
    }

    // ── USER: own dismissed ────────────────────────────────
    @GetMapping("/my/dismissed")
    public ResponseEntity<List<ReminderResponse>> getMyDismissed(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                reminderService.getDismissedRemindersByUsername(userDetails.getUsername()));
    }

    // ── By policy ─────────────────────────────────────────
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<List<ReminderResponse>> getByPolicy(
            @PathVariable String policyId) {
        return ResponseEntity.ok(reminderService.getRemindersByPolicy(policyId));
    }

    // ── Create ────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ReminderResponse> createReminder(
            @RequestBody CreateReminderRequest request) {
        return ResponseEntity.ok(reminderService.createManualReminder(request));
    }

    // ── Auto generate (admin) ─────────────────────────────
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerGeneration() {
        reminderService.generateExpiryReminders();
        reminderService.generatePaymentReminders();
        return ResponseEntity.ok("Reminders generated successfully");
    }

    // ── Dismiss single (admin) ────────────────────────────
    @PutMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReminderResponse> dismiss(@PathVariable String id) {
        return ResponseEntity.ok(reminderService.dismissReminder(id));
    }

    // ── Restore single (admin) ────────────────────────────
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReminderResponse> restore(@PathVariable String id) {
        return ResponseEntity.ok(reminderService.restoreReminder(id));
    }

    // ── Permanent delete (admin) ──────────────────────────
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(@PathVariable String id) {
        reminderService.permanentDeleteReminder(id);
        return ResponseEntity.ok().build();
    }

    // ── Dismiss all for a policy (admin) ──────────────────
    @PutMapping("/policy/{policyId}/dismiss-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> dismissAllByPolicy(@PathVariable String policyId) {
        reminderService.dismissAllByPolicy(policyId);
        return ResponseEntity.ok().build();
    }
}