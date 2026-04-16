package com.insuraTrack.service;

import com.insuraTrack.dto.CreateReminderRequest;
import com.insuraTrack.dto.ReminderResponse;
import com.insuraTrack.enums.ReminderType;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.PremiumPayment;
import com.insuraTrack.model.Reminder;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.repository.PremiumPaymentRepository;
import com.insuraTrack.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final PolicyRepository policyRepository;
    private final PremiumPaymentRepository paymentRepository;
    private final NotificationService notificationService;

    // ==================== ACTIVE ====================

    public List<ReminderResponse> getActiveReminders() {
        return reminderRepository.findByDismissedFalseOrderByReminderDateAsc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ReminderResponse> getActiveRemindersByUsername(String username) {
        return reminderRepository.findByDismissedFalseOrderByReminderDateAsc()
                .stream()
                .filter(r -> r.getPolicy() != null &&
                        r.getPolicy().getUser() != null &&
                        r.getPolicy().getUser().getEmail().equals(username))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== DISMISSED ====================

    public List<ReminderResponse> getDismissedReminders() {
        return reminderRepository.findByDismissedTrueOrderByReminderDateDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ReminderResponse> getDismissedRemindersByUsername(String username) {
        return reminderRepository.findByDismissedTrueOrderByReminderDateDesc()
                .stream()
                .filter(r -> r.getPolicy() != null &&
                        r.getPolicy().getUser() != null &&
                        r.getPolicy().getUser().getEmail().equals(username))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== BY POLICY ====================

    public List<ReminderResponse> getRemindersByPolicy(String policyId) {
        return reminderRepository.findByPolicyId(policyId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==================== ACTIONS ====================

    public ReminderResponse dismissReminder(String id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + id));
        reminder.setDismissed(true);
        reminder = reminderRepository.save(reminder);
        log.info("Reminder dismissed: {}", id);
        return convertToResponse(reminder);
    }

    public ReminderResponse restoreReminder(String id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + id));
        reminder.setDismissed(false);
        reminder = reminderRepository.save(reminder);
        log.info("Reminder restored: {}", id);
        return convertToResponse(reminder);
    }

    public void permanentDeleteReminder(String id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + id));
        reminderRepository.delete(reminder);
        log.info("Reminder permanently deleted: {}", id);
    }

    public void dismissAllByPolicy(String policyId) {
        List<Reminder> reminders = reminderRepository.findByPolicyId(policyId)
                .stream()
                .filter(r -> !r.isDismissed())
                .collect(Collectors.toList());
        reminders.forEach(r -> r.setDismissed(true));
        reminderRepository.saveAll(reminders);
        log.info("Dismissed {} reminders for policy: {}", reminders.size(), policyId);
    }

    public ReminderResponse createManualReminder(CreateReminderRequest request) {
        Policy policy = policyRepository.findActiveById(request.getPolicyId())
                .orElseThrow(() -> new RuntimeException("Policy not found: " + request.getPolicyId()));

        boolean exists = reminderRepository.existsByPolicyIdAndMessageAndDismissedFalse(
                policy.getId(), request.getMessage());
        if (exists) {
            throw new RuntimeException("A similar reminder already exists for this policy");
        }

        Reminder reminder = Reminder.builder()
                .policy(policy)
                .reminderDate(request.getReminderDate())
                .type(request.getType())
                .message(request.getMessage())
                .severity(request.getSeverity() != null ? request.getSeverity() : "MEDIUM")
                .sent(false)
                .dismissed(false)
                .build();

        reminder = reminderRepository.save(reminder);
        log.info("Manual reminder created for policy: {}", policy.getPolicyNumber());

        if (policy.getUser() != null) {
            notificationService.createNotificationForUser(
                    policy.getUser(),
                    "New Reminder: " + request.getType().name(),
                    request.getMessage(),
                    request.getType().name()
            );
        }

        return convertToResponse(reminder);
    }

    // ==================== SCHEDULER ====================

    public void generateExpiryReminders() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        List<Policy> expiringPolicies = policyRepository.findExpiringSoon(today, thirtyDaysLater);

        int expiryCount = 0;
        int notificationCount = 0;

        for (Policy policy : expiringPolicies) {
            long daysLeft = ChronoUnit.DAYS.between(today, policy.getEndDate());
            String severity = getSeverity(policy.getEndDate());
            String message = String.format("⚠️ Policy %s is expiring on %s (%d days left)",
                    policy.getPolicyNumber(), policy.getEndDate(), daysLeft);

            boolean reminderExists = reminderRepository
                    .existsByPolicyIdAndMessageAndDismissedFalse(policy.getId(), message);

            if (!reminderExists) {
                LocalDate reminderDate = daysLeft <= 7 ? today : policy.getEndDate().minusDays(7);

                Reminder reminder = Reminder.builder()
                        .policy(policy)
                        .reminderDate(reminderDate)
                        .type(ReminderType.EXPIRY)
                        .message(message)
                        .severity(severity)
                        .sent(false)
                        .dismissed(false)
                        .build();

                reminderRepository.save(reminder);
                expiryCount++;

                if (policy.getUser() != null) {
                    notificationService.createNotificationForUser(
                            policy.getUser(),
                            "Policy Expiry Alert",
                            message,
                            "EXPIRY"
                    );
                    notificationCount++;
                }

                log.info("Expiry reminder generated for policy: {}", policy.getPolicyNumber());
            }
        }

        log.info("Generated {} expiry reminders, {} notifications", expiryCount, notificationCount);
    }

    public void generatePaymentReminders() {
        LocalDate today = LocalDate.now();
        LocalDate fifteenDaysLater = today.plusDays(15);
        List<PremiumPayment> upcomingPayments = paymentRepository.findUpcoming(fifteenDaysLater);

        int paymentCount = 0;
        int notificationCount = 0;

        for (PremiumPayment payment : upcomingPayments) {
            long daysLeft = ChronoUnit.DAYS.between(today, payment.getDueDate());
            String severity = daysLeft <= 3 ? "HIGH" : (daysLeft <= 7 ? "MEDIUM" : "LOW");
            String message = String.format("💰 Payment of ₹%.2f for policy %s is due on %s (%d days left)",
                    payment.getAmount(), payment.getPolicy().getPolicyNumber(),
                    payment.getDueDate(), daysLeft);

            boolean reminderExists = reminderRepository
                    .existsByPolicyIdAndMessageAndDismissedFalse(payment.getPolicy().getId(), message);

            if (!reminderExists) {
                LocalDate reminderDate = daysLeft <= 3 ? today : payment.getDueDate().minusDays(3);

                Reminder reminder = Reminder.builder()
                        .policy(payment.getPolicy())
                        .reminderDate(reminderDate)
                        .type(ReminderType.PAYMENT)
                        .message(message)
                        .severity(severity)
                        .sent(false)
                        .dismissed(false)
                        .build();

                reminderRepository.save(reminder);
                paymentCount++;

                if (payment.getPolicy().getUser() != null) {
                    notificationService.createNotificationForUser(
                            payment.getPolicy().getUser(),
                            "Payment Due Reminder",
                            message,
                            "PAYMENT"
                    );
                    notificationCount++;
                }

                log.info("Payment reminder generated for policy: {}", payment.getPolicy().getPolicyNumber());
            }
        }

        log.info("Generated {} payment reminders, {} notifications", paymentCount, notificationCount);
    }

    // ==================== HELPERS ====================

    private String getSeverity(LocalDate expiryDate) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        if (daysLeft <= 7) return "HIGH";
        if (daysLeft <= 15) return "MEDIUM";
        return "LOW";
    }

    private ReminderResponse convertToResponse(Reminder reminder) {
        return ReminderResponse.builder()
                .id(reminder.getId())
                .policyId(reminder.getPolicy() != null ? reminder.getPolicy().getId() : null)
                .policyNumber(reminder.getPolicy() != null ? reminder.getPolicy().getPolicyNumber() : null)
                .reminderDate(reminder.getReminderDate())
                .type(reminder.getType() != null ? reminder.getType().name() : null)
                .message(reminder.getMessage())
                .severity(reminder.getSeverity())
                .sent(reminder.isSent())
                .dismissed(reminder.isDismissed())
                .createdAt(reminder.getCreatedAt())
                .build();
    }
}