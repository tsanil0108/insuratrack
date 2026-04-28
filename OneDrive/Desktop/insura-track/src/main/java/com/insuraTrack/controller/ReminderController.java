package com.insuraTrack.controller;

import com.insuraTrack.model.Reminder;
import com.insuraTrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    // ── CREATE ─────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reminder> create(@RequestBody Reminder reminder) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reminderService.create(reminder));
    }

    // ── READ — ADMIN ONLY ──────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reminder>> getAll() {
        return ResponseEntity.ok(reminderService.getAll());
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reminder>> getActive() {
        return ResponseEntity.ok(reminderService.getActive());
    }

    @GetMapping("/dismissed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reminder>> getDismissed() {
        return ResponseEntity.ok(reminderService.getDismissed());
    }

    // ── READ — BOTH ROLES ──────────────────────────────────

    /**
     * ✅ FIX: Was ADMIN-only → now ADMIN + USER.
     *
     * For ADMIN  → returns ALL active (non-dismissed) reminders across all policies.
     * For USER   → the service impl checks the role and returns only that user's reminders,
     *              so one endpoint serves both roles cleanly.
     *
     * This is the endpoint the frontend dashboard calls on startup.
     * Returning 403 here was causing the false "Session expired" toast.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Reminder>> getPending() {
        return ResponseEntity.ok(reminderService.getPendingReminders());
    }

    /**
     * USER-scoped active reminders (only this user's policies).
     * Kept for backward compat / explicit user-facing calls.
     */
    @GetMapping("/my/active")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Reminder>> getMyActive() {
        return ResponseEntity.ok(reminderService.getMyActive());
    }

    /**
     * USER-scoped dismissed reminders.
     */
    @GetMapping("/my/dismissed")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Reminder>> getMyDismissed() {
        return ResponseEntity.ok(reminderService.getMyDismissed());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Reminder> getById(@PathVariable String id) {
        return ResponseEntity.ok(reminderService.getById(id));
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Reminder>> getByPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(reminderService.getByPolicy(policyId));
    }

    // ── ACTIONS — BOTH ROLES ───────────────────────────────

    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Reminder> dismiss(@PathVariable String id) {
        return ResponseEntity.ok(reminderService.dismiss(id));
    }

    // ── ACTIONS — ADMIN ONLY ───────────────────────────────

    @PatchMapping("/{id}/sent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reminder> markAsSent(@PathVariable String id) {
        return ResponseEntity.ok(reminderService.markAsSent(id));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reminder> restore(@PathVariable String id) {
        return ResponseEntity.ok(reminderService.restore(id));
    }

    @PatchMapping("/policy/{policyId}/dismiss-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> dismissAllByPolicy(@PathVariable String policyId) {
        reminderService.dismissAllByPolicy(policyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generateAll() {
        reminderService.generateAll();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/policy/{policyId}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generateForPolicy(@PathVariable String policyId) {
        reminderService.generateForPolicy(policyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestParam String deletedBy) {
        reminderService.softDelete(id, deletedBy);
        return ResponseEntity.noContent().build();
    }
}