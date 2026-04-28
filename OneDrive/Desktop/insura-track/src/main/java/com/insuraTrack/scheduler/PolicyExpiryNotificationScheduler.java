package com.insuraTrack.scheduler;

import com.insuraTrack.model.Policy;
import com.insuraTrack.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler that checks for expiring/expired policies and sends notifications.
 *
 * ✅ FIX: All methods that access lazy-loaded associations (e.g. policy.getCompany().getName())
 *         are annotated with @Transactional so the Hibernate session stays open.
 *         This eliminates the LazyInitializationException seen on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyExpiryNotificationScheduler {

    private final PolicyRepository policyRepository;

    // ── Triggered once 30 seconds after startup ──────────────────────────────
    @PostConstruct
    public void scheduleInitialCheck() {
        // Run in a separate thread so startup is not blocked
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(30_000); // wait 30s for app to fully start
                runOnStartup();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(true);
        t.setName("policy-expiry-startup-check");
        t.start();
    }

    /**
     * Runs once on startup (via @PostConstruct thread above).
     * ✅ @Transactional keeps the Hibernate session open so lazy associations load correctly.
     */
    @Transactional(readOnly = true)
    public void runOnStartup() {
        log.info("Running initial policy expiry check on startup...");
        checkPolicyExpiry();
    }

    /**
     * Scheduled daily at 8:00 AM.
     * ✅ @Transactional keeps the Hibernate session open.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void scheduledDailyCheck() {
        log.info("Running scheduled daily policy expiry check...");
        checkPolicyExpiry();
    }

    /**
     * Core check logic — must always be called within a transaction.
     * Accesses lazy associations (Company.getName(), etc.) safely.
     */
    private void checkPolicyExpiry() {
        log.info("Running policy expiry notification check...");

        LocalDate today    = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        LocalDate in7Days  = today.plusDays(7);

        try {
            // Policies expiring in the next 30 days
            List<Policy> expiringSoon = policyRepository.findExpiringBetween(today, in30Days);
            log.info("Found {} policies expiring in next 30 days", expiringSoon.size());

            for (Policy policy : expiringSoon) {
                try {
                    // ✅ Safe: within @Transactional, lazy proxy initializes correctly
                    String companyName = policy.getCompany() != null
                            ? policy.getCompany().getName()
                            : "Unknown Company";

                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, policy.getEndDate());

                    if (daysLeft <= 7) {
                        log.warn("⚠️  URGENT — Policy {} ({}) expires in {} day(s) on {}",
                                policy.getPolicyNumber(), companyName, daysLeft, policy.getEndDate());
                        sendUrgentNotification(policy, companyName, daysLeft);
                    } else {
                        log.info("📅 Policy {} ({}) expires in {} days on {}",
                                policy.getPolicyNumber(), companyName, daysLeft, policy.getEndDate());
                        sendReminderNotification(policy, companyName, daysLeft);
                    }
                } catch (Exception e) {
                    log.error("Error processing policy {}: {}", policy.getId(), e.getMessage());
                }
            }

            // Policies already expired
            List<Policy> expired = policyRepository.findExpiringBetween(today.minusYears(1), today.minusDays(1));
            log.info("Found {} recently expired policies", expired.size());

        } catch (Exception e) {
            log.error("Policy expiry check failed: {}", e.getMessage(), e);
        }
    }

    private void sendUrgentNotification(Policy policy, String companyName, long daysLeft) {
        // TODO: Integrate with your email/notification service
        // Example: notificationService.sendUrgentExpiryAlert(policy, companyName, daysLeft);
        log.info("[NOTIFICATION] URGENT expiry alert for policy {} - {} - {} days left",
                policy.getPolicyNumber(), companyName, daysLeft);
    }

    private void sendReminderNotification(Policy policy, String companyName, long daysLeft) {
        // TODO: Integrate with your email/notification service
        // Example: notificationService.sendExpiryReminder(policy, companyName, daysLeft);
        log.info("[NOTIFICATION] Reminder for policy {} - {} - {} days left",
                policy.getPolicyNumber(), companyName, daysLeft);
    }
}