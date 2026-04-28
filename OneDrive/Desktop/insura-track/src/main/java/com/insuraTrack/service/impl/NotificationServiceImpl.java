package com.insuraTrack.service.impl;

import com.insuraTrack.dto.NotificationDTO;
import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.Notification;
import com.insuraTrack.model.User;
import com.insuraTrack.repository.NotificationRepository;
import com.insuraTrack.repository.UserRepository;
import com.insuraTrack.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Notification createNotification(String userId, String title,
                                           String message, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * ✅ Creates a notification only if one of the same type hasn't already
     * been sent today for the same policy number — prevents daily duplicates.
     *
     * @return true if created, false if skipped (duplicate)
     */
    @Override
    @Transactional
    public boolean createIfNotDuplicate(String userId, String title, String message,
                                        String type, String policyNumber) {
        boolean alreadySent = notificationRepository
                .existsTodayForPolicyAndUser(userId, type, policyNumber);

        if (alreadySent) {
            return false;
        }

        createNotification(userId, title, message, type);
        return true;
    }

    @Override
    public List<NotificationDTO> getByUser(String userId) {
        return notificationRepository
                .findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::from)
                .toList();
    }

    @Override
    public List<NotificationDTO> getUnreadByUser(String userId) {
        return notificationRepository
                .findAllByUserIdAndReadFalseAndDeletedFalse(userId)
                .stream()
                .map(NotificationDTO::from)
                .toList();
    }

    @Override
    public long countUnread(String userId) {
        return notificationRepository.countByUserIdAndReadFalseAndDeletedFalse(userId);
    }

    @Override
    @Transactional
    public NotificationDTO markAsRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .filter(n -> !n.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());

        return NotificationDTO.from(notificationRepository.save(notification));
    }

    /**
     * ✅ Uses a bulk UPDATE query instead of load-all + saveAll
     *    for better performance with large unread counts.
     */
    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional
    public void softDelete(String id, String deletedBy) {
        Notification notification = notificationRepository.findById(id)
                .filter(n -> !n.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        notification.softDelete(deletedBy);
        notificationRepository.save(notification);
    }
}