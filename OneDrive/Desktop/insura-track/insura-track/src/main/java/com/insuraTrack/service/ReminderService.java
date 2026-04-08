package com.insuraTrack.service;

import com.insuraTrack.dto.ReminderResponse;
import com.insuraTrack.enums.ReminderType;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.PremiumPayment;
import com.insuraTrack.model.Reminder;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.repository.PremiumPaymentRepository;
import com.insuraTrack.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final PolicyRepository policyRepository;
    private final PremiumPaymentRepository paymentRepository;

    // ==================== CONTROLLER REQUIRED METHODS ====================

    // Get all active reminders (not dismissed)
    public List<ReminderResponse> getActiveReminders() {
        return reminderRepository.findAll().stream()
                .filter(reminder -> !reminder.isDismissed())
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Get reminders by policy ID
    public List<ReminderResponse> getRemindersByPolicy(String policyId) {
        return reminderRepository.findAll().stream()
                .filter(reminder -> reminder.getPolicy() != null &&
                        reminder.getPolicy().getId().equals(policyId))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Dismiss a single reminder
    public ReminderResponse dismissReminder(String id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        reminder.setDismissed(true);
        reminder = reminderRepository.save(reminder);
        return convertToResponse(reminder);
    }

    // Dismiss all reminders for a policy
    public void dismissAllByPolicy(String policyId) {
        List<Reminder> reminders = reminderRepository.findAll().stream()
                .filter(reminder -> reminder.getPolicy() != null &&
                        reminder.getPolicy().getId().equals(policyId) &&
                        !reminder.isDismissed())
                .toList();

        for (Reminder reminder : reminders) {
            reminder.setDismissed(true);
            reminderRepository.save(reminder);
        }
    }

    // ==================== SCHEDULER METHODS ====================

    // Generate expiry reminders for policies expiring in next 7 days
    public void generateExpiryReminders() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        List<Policy> expiringPolicies = policyRepository.findAll().stream()
                .filter(policy -> policy.getEndDate() != null &&
                        policy.getEndDate().isAfter(today) &&
                        policy.getEndDate().isBefore(sevenDaysLater) &&
                        policy.getStatus() != com.insuraTrack.enums.PolicyStatus.EXPIRED)
                .toList();

        for (Policy policy : expiringPolicies) {
            boolean reminderExists = reminderRepository.findAll().stream()
                    .anyMatch(r -> r.getPolicy() != null &&
                            r.getPolicy().getId().equals(policy.getId()) &&
                            r.getType() == ReminderType.EXPIRY &&
                            !r.isDismissed());

            if (!reminderExists) {
                Reminder reminder = Reminder.builder()
                        .policy(policy)
                        .reminderDate(policy.getEndDate().minusDays(3))
                        .type(ReminderType.EXPIRY)
                        .message("Your policy " + policy.getPolicyNumber() + " is expiring on " + policy.getEndDate())
                        .severity(getSeverity(policy.getEndDate()))
                        .sent(false)
                        .dismissed(false)
                        .build();

                reminderRepository.save(reminder);
                System.out.println("Expiry reminder generated for policy: " + policy.getPolicyNumber());
            }
        }
    }

    // Generate payment reminders for unpaid payments due in next 7 days
    public void generatePaymentReminders() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        List<PremiumPayment> unpaidPayments = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == com.insuraTrack.enums.PaymentStatus.UNPAID &&
                        payment.getDueDate() != null &&
                        payment.getDueDate().isAfter(today) &&
                        payment.getDueDate().isBefore(sevenDaysLater))
                .toList();

        for (PremiumPayment payment : unpaidPayments) {
            boolean reminderExists = reminderRepository.findAll().stream()
                    .anyMatch(r -> r.getPolicy() != null &&
                            r.getPolicy().getId().equals(payment.getPolicy().getId()) &&
                            r.getType() == ReminderType.PAYMENT &&
                            !r.isDismissed());

            if (!reminderExists) {
                Reminder reminder = Reminder.builder()
                        .policy(payment.getPolicy())
                        .reminderDate(payment.getDueDate().minusDays(3))
                        .type(ReminderType.PAYMENT)
                        .message("Payment of ₹" + payment.getAmount() + " is due for policy " +
                                payment.getPolicy().getPolicyNumber() + " on " + payment.getDueDate())
                        .severity("HIGH")
                        .sent(false)
                        .dismissed(false)
                        .build();

                reminderRepository.save(reminder);
                System.out.println("Payment reminder generated for policy: " + payment.getPolicy().getPolicyNumber());
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private String getSeverity(LocalDate expiryDate) {
        LocalDate today = LocalDate.now();
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);

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