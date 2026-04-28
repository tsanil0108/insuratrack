package com.insuraTrack.repository;

import com.insuraTrack.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    // All non-deleted notifications for a user, newest first
    List<Notification> findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);

    // Unread + non-deleted notifications for a user
    List<Notification> findAllByUserIdAndReadFalseAndDeletedFalse(String userId);

    // Count of unread + non-deleted for a user
    long countByUserIdAndReadFalseAndDeletedFalse(String userId);

    // ✅ Check if a notification of a specific type already exists for a policy
    // Used to prevent duplicate notifications for the same policy on the same day
    @Query("""
        SELECT COUNT(n) > 0 FROM Notification n
        WHERE n.user.id = :userId
          AND n.type = :type
          AND n.message LIKE %:policyNumber%
          AND n.deleted = false
          AND CAST(n.createdAt AS date) = CURRENT_DATE
        """)
    boolean existsTodayForPolicyAndUser(
            @Param("userId") String userId,
            @Param("type") String type,
            @Param("policyNumber") String policyNumber
    );

    // ✅ Bulk mark-as-read via query (more efficient than load-all + saveAll)
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true, n.readAt = CURRENT_TIMESTAMP
        WHERE n.user.id = :userId AND n.read = false AND n.deleted = false
        """)
    void markAllAsReadByUserId(@Param("userId") String userId);
}