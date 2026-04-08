package com.insuraTrack.repository;

import com.insuraTrack.enums.ReminderType;
import com.insuraTrack.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, String> {

    List<Reminder> findByPolicyId(String policyId);

    List<Reminder> findBySentFalseAndReminderDateLessThanEqual(LocalDate date);

    List<Reminder> findByDismissedFalseOrderByReminderDateAsc();

    @Query("SELECT r FROM Reminder r WHERE r.dismissed = false AND r.policy.company.id = :companyId ORDER BY r.reminderDate ASC")
    List<Reminder> findActiveByCompanyId(@Param("companyId") String companyId);

    // FIX #6: Parameter type is ReminderType enum, not String
    @Query("SELECT r FROM Reminder r WHERE r.policy.id = :policyId AND r.type = :type")
    List<Reminder> findByPolicyIdAndType(@Param("policyId") String policyId,
                                         @Param("type") ReminderType type);

    boolean existsByPolicyIdAndMessageAndDismissedFalse(String policyId, String message);
}