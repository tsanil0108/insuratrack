package com.insuraTrack.service.impl;

import com.insuraTrack.enums.ReminderType;
import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.Reminder;
import com.insuraTrack.model.User;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.repository.ReminderRepository;
import com.insuraTrack.repository.UserRepository;
import com.insuraTrack.service.NotificationService;
import com.insuraTrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;

    // ── Auth helpers ─────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Returns true if the currently authenticated principal has ROLE_ADMIN.
     */
    private boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Reminder create(Reminder reminder) {
        validateReminder(reminder);
        return reminderRepository.save(reminder);
    }

    @Override
    public Reminder getById(String id) {
        return reminderRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + id));
    }

    @Override
    public List<Reminder> getAll() {
        return reminderRepository.findAllActive();
    }

    @Override
    public List<Reminder> getByPolicy(String policyId) {
        return reminderRepository.findActiveByPolicyId(policyId);
    }

    /**
     * ✅ FIX: Role-aware pending reminders.
     *
     * Previously this always returned ALL reminders → caused 403 when called
     * by a USER (endpoint was ADMIN-only).
     *
     * Now:
     *  - ADMIN  → all active (non-dismissed, non-deleted) reminders
     *  - USER   → only reminders belonging to that user's policies
     *
     * The controller endpoint is now open to both roles, so no more 403.
     */
    @Override
    public List<Reminder> getPendingReminders() {
        if (currentUserIsAdmin()) {
            return reminderRepository.findAllActiveReminders();
        }
        // USER role — return only their own reminders
        try {
            User user = getCurrentUser();
            return reminderRepository.findActiveByUserId(user.getId());
        } catch (Exception e) {
            log.warn("Could not resolve current user for pending reminders: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── State mutations ───────────────────────────────────────────────

    @Override
    @Transactional
    public Reminder markAsSent(String id) {
        Reminder reminder = getById(id);
        reminder.setSent(true);
        return reminderRepository.save(reminder);
    }

    @Override
    @Transactional
    public Reminder dismiss(String id) {
        Reminder reminder = getById(id);
        reminder.setDismissed(true);
        return reminderRepository.save(reminder);
    }

    @Override
    @Transactional
    public void softDelete(String id, String deletedByEmail) {
        Reminder reminder = getById(id);
        reminder.softDelete(deletedByEmail);
        reminderRepository.save(reminder);
    }

    // ── Scheduled processor ───────────────────────────────────────────

    @Override
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processScheduledReminders() {
        log.info("Processing scheduled reminders...");
        LocalDate today = LocalDate.now();
        List<Reminder> dueReminders = reminderRepository.findDueReminders(today);

        int processedCount = 0;
        for (Reminder reminder : dueReminders) {
            try {
                if (reminder.getPolicy() != null && reminder.getPolicy().getUser() != null) {
                    notificationService.createNotification(
                            reminder.getPolicy().getUser().getId(),
                            "Policy Reminder: " + reminder.getType(),
                            reminder.getMessage(),
                            reminder.getType().name()
                    );
                    reminder.setSent(true);
                    reminderRepository.save(reminder);
                    processedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to process reminder {}: {}", reminder.getId(), e.getMessage());
            }
        }
        log.info("Processed {} out of {} due reminders.", processedCount, dueReminders.size());
    }

    // ── Scoped list methods ───────────────────────────────────────────

    @Override
    public List<Reminder> getActive() {
        return reminderRepository.findAllActiveReminders();
    }

    @Override
    public List<Reminder> getDismissed() {
        return reminderRepository.findAllDismissedReminders();
    }

    @Override
    public List<Reminder> getMyActive() {
        User currentUser = getCurrentUser();
        return reminderRepository.findActiveByUserId(currentUser.getId());
    }

    @Override
    public List<Reminder> getMyDismissed() {
        User currentUser = getCurrentUser();
        return reminderRepository.findDismissedByUserId(currentUser.getId());
    }

    @Override
    @Transactional
    public Reminder restore(String id) {
        Reminder reminder = getById(id);
        if (reminder.isDismissed()) {
            reminder.setDismissed(false);
            reminder = reminderRepository.save(reminder);
            log.info("Restored reminder: {}", id);
        }
        return reminder;
    }

    // ── Batch / generation — REQUIRES_NEW so they never poison ────────
    // the caller's (PolicyService) transaction

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dismissAllByPolicy(String policyId) {
        int updatedCount = reminderRepository.dismissAllByPolicyId(policyId);
        log.info("Dismissed {} reminders for policy: {}", updatedCount, policyId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAll() {
        log.info("Generating reminders for all active policies...");
        List<Policy> policies = policyRepository.findAllByDeletedFalse();
        int totalGenerated = 0;

        for (Policy policy : policies) {
            totalGenerated += generateForPolicyInternal(policy);
        }

        log.info("Generated {} reminders for {} policies", totalGenerated, policies.size());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateForPolicy(String policyId) {
        Policy policy = policyRepository.findByIdAndDeletedFalse(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        int generatedCount = generateForPolicyInternal(policy);
        log.info("Generated {} reminders for policy: {}", generatedCount, policy.getPolicyNumber());
    }

    // ── Private helpers ───────────────────────────────────────────────

    private int generateForPolicyInternal(Policy policy) {
        int generatedCount = 0;
        LocalDate today = LocalDate.now();
        LocalDate endDate = policy.getEndDate();

        if (endDate == null || !endDate.isAfter(today)) return 0;

        long daysUntilExpiry = today.until(endDate, ChronoUnit.DAYS);

        int[]    daysArray     = { 30, 15, 7, 3, 1 };
        String[] severityArray = { "MEDIUM", "HIGH", "HIGH", "HIGH", "HIGH" };
        String[] messageArray  = {
                "Policy expires in 30 days",
                "Policy expires in 15 days",
                "Policy expires in 7 days",
                "Policy expires in 3 days",
                "Policy expires TOMORROW!"
        };

        for (int i = 0; i < daysArray.length; i++) {
            int    daysBefore      = daysArray[i];
            String severity        = severityArray[i];
            String messageTemplate = messageArray[i];

            if (daysUntilExpiry >= daysBefore) {
                LocalDate reminderDate = endDate.minusDays(daysBefore);
                String    message      = messageTemplate + ": " + policy.getPolicyNumber();

                if (!reminderRepository.existsByPolicyAndDateAndType(
                        policy.getId(), reminderDate, ReminderType.EXPIRY)) {

                    Reminder reminder = Reminder.builder()
                            .policy(policy)
                            .reminderDate(reminderDate)
                            .dueDate(endDate)
                            .type(ReminderType.EXPIRY)
                            .message(message)
                            .severity(severity)
                            .sent(false)
                            .dismissed(false)
                            .build();

                    reminderRepository.save(reminder);
                    generatedCount++;
                    log.debug("Created reminder for policy {} on {}",
                            policy.getPolicyNumber(), reminderDate);
                }
            }
        }
        return generatedCount;
    }

    private void validateReminder(Reminder reminder) {
        if (reminder.getReminderDate() == null) {
            throw new IllegalArgumentException("Reminder date is required");
        }
        if (reminder.getType() == null) {
            throw new IllegalArgumentException("Reminder type is required");
        }
        if (reminder.getMessage() == null || reminder.getMessage().isBlank()) {
            throw new IllegalArgumentException("Reminder message is required");
        }
    }
}