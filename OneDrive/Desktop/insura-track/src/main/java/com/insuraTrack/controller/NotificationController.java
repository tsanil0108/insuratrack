package com.insuraTrack.controller;

import com.insuraTrack.dto.NotificationDTO;
import com.insuraTrack.model.User;
import com.insuraTrack.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/v1/notifications/my  — returns current user's notifications
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<NotificationDTO>> getMy(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getByUser(resolveUserId(authentication)));
    }

    // GET /api/v1/notifications/user/{userId}
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<NotificationDTO>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    // GET /api/v1/notifications/user/{userId}/unread
    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<NotificationDTO>> getUnread(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getUnreadByUser(userId));
    }

    // GET /api/v1/notifications/user/{userId}/unread/count
    @GetMapping("/user/{userId}/unread/count")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Long> countUnread(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.countUnread(userId));
    }

    // PATCH /api/v1/notifications/{id}/read
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // PATCH /api/v1/notifications/read-all  — marks all as read for current user
    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(resolveUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/notifications/user/{userId}/read-all
    @PatchMapping("/user/{userId}/read-all")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Void> markAllAsReadByUser(@PathVariable String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/v1/notifications/{id}?deletedBy=email@example.com
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @RequestParam String deletedBy) {
        notificationService.softDelete(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    // ✅ Centralised user ID resolution from Authentication
    private String resolveUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return authentication.getName();
    }
}