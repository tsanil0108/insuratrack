package com.insuraTrack.repository;

import com.insuraTrack.enums.ReminderType;
import com.insuraTrack.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, String> {

    // ✅ ADD THIS METHOD - For DashboardService
    @Query("SELECT r FROM Reminder r WHERE r.deleted = false")
    List<Reminder> findAllByDeletedFalse();

    // ─── Basic Queries ────────────────────────────────────────────────

    @Query("SELECT r FROM Reminder r WHERE r.deleted = false")
    List<Reminder> findAllActive();

    @Query("SELECT r FROM Reminder r WHERE r.id = :id AND r.deleted = false")
    Optional<Reminder> findActiveById(@Param("id") String id);

    @Query("SELECT r FROM Reminder r WHERE r.policy.id = :policyId AND r.deleted = false")
    List<Reminder> findByPolicyId(@Param("policyId") String policyId);

    @Query("SELECT r FROM Reminder r WHERE r.policy.id = :policyId AND r.dismissed = false AND r.deleted = false")
    List<Reminder> findActiveByPolicyId(@Param("policyId") String policyId);

    // ─── Scheduled Reminders ─────────────────────────────────────────

    @Query("SELECT r FROM Reminder r WHERE r.sent = false AND r.dismissed = false AND r.deleted = false AND r.reminderDate <= :date")
    List<Reminder> findDueReminders(@Param("date") LocalDate date);

    // ─── Active/Dismissed Reminders ──────────────────────────────────

    @Query("SELECT r FROM Reminder r WHERE r.dismissed = false AND r.deleted = false")
    List<Reminder> findAllActiveReminders();

    @Query("SELECT r FROM Reminder r WHERE r.dismissed = true AND r.deleted = false")
    List<Reminder> findAllDismissedReminders();

    // ─── User-specific Queries ───────────────────────────────────────

    @Query("SELECT r FROM Reminder r WHERE r.policy.user.id = :userId AND r.dismissed = false AND r.deleted = false")
    List<Reminder> findActiveByUserId(@Param("userId") String userId);

    @Query("SELECT r FROM Reminder r WHERE r.policy.user.id = :userId AND r.dismissed = true AND r.deleted = false")
    List<Reminder> findDismissedByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.policy.user.id = :userId AND r.dismissed = false AND r.sent = false AND r.deleted = false")
    long countActiveByUserId(@Param("userId") String userId);

    // ─── Existence Checks ────────────────────────────────────────────

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reminder r " +
            "WHERE r.policy.id = :policyId AND r.reminderDate = :reminderDate AND r.type = :type AND r.deleted = false")
    boolean existsByPolicyAndDateAndType(@Param("policyId") String policyId,
                                         @Param("reminderDate") LocalDate reminderDate,
                                         @Param("type") ReminderType type);

    // ─── Batch Operations ────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Reminder r SET r.dismissed = true WHERE r.policy.id = :policyId AND r.dismissed = false AND r.deleted = false")
    int dismissAllByPolicyId(@Param("policyId") String policyId);

    @Modifying
    @Query("UPDATE Reminder r SET r.sent = true WHERE r.id IN :ids")
    int markAsSentBatch(@Param("ids") List<String> ids);

    // ─── Count Queries ───────────────────────────────────────────────

    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.dismissed = false AND r.deleted = false")
    long countActiveReminders();

    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.dismissed = false AND r.reminderDate <= :date AND r.deleted = false")
    long countDueReminders(@Param("date") LocalDate date);
}