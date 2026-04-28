package com.insuraTrack.service;

import com.insuraTrack.dto.NotificationDTO;
import com.insuraTrack.model.Notification;

import java.util.List;

public interface NotificationService {

    Notification createNotification(String userId, String title, String message, String type);

    // ✅ Idempotent version — skips if already sent today for same policy+type
    boolean createIfNotDuplicate(String userId, String title, String message,
                                 String type, String policyNumber);

    List<NotificationDTO> getByUser(String userId);

    List<NotificationDTO> getUnreadByUser(String userId);

    long countUnread(String userId);

    NotificationDTO markAsRead(String id);

    void markAllAsRead(String userId);

    void softDelete(String id, String deletedBy);
}