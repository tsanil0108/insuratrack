package com.insuraTrack.controller;

import com.insuraTrack.model.User;
import com.insuraTrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** ADMIN only — create a new user */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user));
    }

    /** ADMIN only — list all users */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * FIX: Added /stats endpoint that frontend users.js calls.
     * Computes stats from the active users list — no separate DB query needed.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        List<User> all = userService.getAllUsers();
        long total       = all.size();
        long active      = all.stream().filter(User::isActive).count();
        long inactive    = total - active;
        long adminCount  = all.stream().filter(u -> u.getRole() != null && u.getRole().name().equals("ADMIN")).count();
        long userCount   = total - adminCount;

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers",   total);
        stats.put("activeUsers",  active);
        stats.put("inactiveUsers",inactive);
        stats.put("adminCount",   adminCount);
        stats.put("userCount",    userCount);
        return ResponseEntity.ok(stats);
    }

    /** ADMIN or self */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /** ADMIN or self */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    /** ADMIN only — soft delete */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String id,
            @RequestParam String deletedBy) {
        userService.softDeleteUser(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    /** ADMIN only — restore / activate deleted user */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable String id) {
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * FIX: Added /toggle-active endpoint called by users.js toggleUserStatus().
     * Toggles active flag without requiring a full user object in the body.
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleActiveUser(@PathVariable String id) {
        userService.toggleActive(id);
        return ResponseEntity.noContent().build();
    }
}