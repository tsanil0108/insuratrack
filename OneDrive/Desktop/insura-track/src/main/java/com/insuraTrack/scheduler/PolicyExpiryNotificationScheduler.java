package com.insuraTrack.scheduler;

import com.insuraTrack.dto.PolicyResponse;
import com.insuraTrack.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyExpiryNotificationScheduler {

    // ✅ Use Service instead of Repository directly
    private final PolicyService policyService;

    @PostConstruct
    public void scheduleInitialCheck() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(30_000);
                runOnStartup();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(true);
        t.setName("policy-expiry-startup-check");
        t.start();
    }

    public void runOnStartup() {
        log.info("Running initial policy expiry check on startup...");
        checkPolicyExpiry();
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void scheduledDailyCheck() {
        log.info("Running scheduled daily policy expiry check...");
        checkPolicyExpiry();
    }

    private void checkPolicyExpiry() {
        log.info("Running policy expiry notification check...");

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);

        try {
            // ✅ Use DTOs instead of Entities (no lazy loading issues)
            List<PolicyResponse> expiringSoon = policyService.getExpiringPolicies(30);
            log.info("Found {} policies expiring in next 30 days", expiringSoon.size());

            for (PolicyResponse policy : expiringSoon) {
                try {
                    String companyName = policy.getCompanyName() != null
                            ? policy.getCompanyName()
                            : "Unknown Company";

                    long daysLeft = 0;
                    if (policy.getEndDate() != null) {
                        daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, policy.getEndDate());
                    }

                    if (daysLeft <= 7 && daysLeft > 0) {
                        log.warn("⚠️ URGENT — Policy {} ({}) expires in {} day(s) on {}",
                                policy.getPolicyNumber(), companyName, daysLeft, policy.getEndDate());
                        sendUrgentNotification(policy, companyName, daysLeft);
                    } else if (daysLeft > 0) {
                        log.info("📅 Policy {} ({}) expires in {} days on {}",
                                policy.getPolicyNumber(), companyName, daysLeft, policy.getEndDate());
                        sendReminderNotification(policy, companyName, daysLeft);
                    }
                } catch (Exception e) {
                    log.error("Error processing policy {}: {}", policy.getPolicyNumber(), e.getMessage());
                }
            }

            // ✅ Get expired policies
            List<PolicyResponse> expired = policyService.getPoliciesByStatus(com.insuraTrack.enums.PolicyStatus.EXPIRED);
            log.info("Found {} expired policies", expired.size());

        } catch (Exception e) {
            log.error("Policy expiry check failed: {}", e.getMessage(), e);
        }
    }

    private void sendUrgentNotification(PolicyResponse policy, String companyName, long daysLeft) {
        log.info("[NOTIFICATION] URGENT expiry alert for policy {} - {} - {} days left",
                policy.getPolicyNumber(), companyName, daysLeft);
    }

    private void sendReminderNotification(PolicyResponse policy, String companyName, long daysLeft) {
        log.info("[NOTIFICATION] Reminder for policy {} - {} - {} days left",
                policy.getPolicyNumber(), companyName, daysLeft);
    }
}