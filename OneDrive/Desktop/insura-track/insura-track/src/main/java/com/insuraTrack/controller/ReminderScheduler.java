package com.insuraTrack.controller;

import com.insuraTrack.model.Policy;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.service.PaymentService;
import com.insuraTrack.service.PolicyService;
import com.insuraTrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final PolicyService policyService;
    private final PaymentService paymentService;
    private final ReminderService reminderService;
    private final PolicyRepository policyRepository;

    // Every day at 8 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyJobs() {
        log.info("Running daily scheduler jobs...");
        updateExpiredPolicies();
        updateExpiringSoonStatus();
        markOverduePayments();
        generateExpiryReminders();
        generatePaymentReminders();
    }

    private void updateExpiredPolicies() {
        policyService.updateExpiredPolicies();
        log.info("Expired policies updated.");
    }

    private void updateExpiringSoonStatus() {
        policyService.updateExpiringSoonStatus();
        log.info("Expiring-soon policy statuses updated.");
    }

    private void markOverduePayments() {
        paymentService.markOverduePayments();
        log.info("Overdue payments marked.");
    }

    private void generateExpiryReminders() {
        reminderService.generateExpiryReminders();
        log.info("Expiry reminders generated.");
    }

    private void generatePaymentReminders() {
        reminderService.generatePaymentReminders();
        log.info("Payment reminders generated.");
    }
}