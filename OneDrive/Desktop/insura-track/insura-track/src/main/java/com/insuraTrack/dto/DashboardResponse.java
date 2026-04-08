package com.insuraTrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    // User Stats
    private long totalUsers;
    private long activeUsers;
    private long adminUsers;

    // Company Stats
    private long totalCompanies;
    private long activeCompanies;

    // Insurance Stats
    private long totalInsuranceTypes;
    private long totalInsuranceProviders;

    // Policy Stats
    private long totalPolicies;
    private long activePolicies;
    private long expiredPolicies;
    private long expiringSoonPolicies;

    // Payment Stats
    private double totalPremiumAmount;
    private long totalPayments;
    private long paidPayments;
    private long unpaidPayments;
    private long overduePayments;

    // Charts Data
    private List<Map<String, Object>> policiesByType;
    private List<Map<String, Object>> paymentsByMonth;
    private List<Map<String, Object>> recentActivities;

    // Reminders
    private long totalReminders;
    private long pendingReminders;
}