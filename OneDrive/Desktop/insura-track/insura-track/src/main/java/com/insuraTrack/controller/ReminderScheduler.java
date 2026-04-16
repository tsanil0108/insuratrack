package com.insuraTrack.controller;

import com.insuraTrack.service.PaymentService;
import com.insuraTrack.service.PolicyService;
import com.insuraTrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final PolicyService policyService;
    private final PaymentService paymentService;
    private final ReminderService reminderService;

    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyJobs() {
        log.info("Running daily scheduler jobs...");
        updateExpiredPolicies();
        updateExpiringSoonStatus();
        markOverduePayments();
        generateExpiryReminders();
        generatePaymentReminders();
        log.info("Daily scheduler jobs completed.");
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
        log.info("Payment reminders generated."); // was missing
    }
}