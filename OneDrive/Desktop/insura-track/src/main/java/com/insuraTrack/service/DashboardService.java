package com.insuraTrack.service;

import com.insuraTrack.dto.DashboardResponse;
import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.enums.Role;
import com.insuraTrack.model.*;
import com.insuraTrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // ✅ ADD THIS IMPORT

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final InsuranceTypeRepository insuranceTypeRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final PolicyRepository policyRepository;
    private final ReminderRepository reminderRepository;

    @Transactional(readOnly = true)  // ✅ ADD THIS ANNOTATION
    public DashboardResponse getDashboard() {

        // User Stats
        List<User> allUsers = userRepository.findAllByDeletedFalse();
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::isActive).count();
        long adminUsers = allUsers.stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .count();

        // Company Stats
        List<Company> allCompanies = companyRepository.findAllByDeletedFalseAndActiveTrue();
        long totalCompanies = allCompanies.size();
        long activeCompanies = allCompanies.stream().filter(Company::isActive).count();

        // Insurance Stats
        long totalInsuranceTypes = insuranceTypeRepository.count();
        long totalInsuranceProviders = insuranceProviderRepository.count();

        // Policies
        List<Policy> allPolicies = policyRepository.findAllByDeletedFalse();

        // ✅ Optional: Force initialize lazy associations to avoid any proxy issues
        allPolicies.forEach(policy -> {
            if (policy.getInsuranceType() != null) {
                policy.getInsuranceType().getName(); // Initialize proxy
            }
        });

        long totalPolicies = allPolicies.size();
        long activePolicies = allPolicies.stream()
                .filter(p -> p.getStatus() == PolicyStatus.ACTIVE)
                .count();
        long expiredPolicies = allPolicies.stream()
                .filter(p -> p.getStatus() == PolicyStatus.EXPIRED)
                .count();
        long expiringSoonPolicies = allPolicies.stream()
                .filter(p -> p.getStatus() == PolicyStatus.EXPIRING_SOON)
                .count();
        double totalPremiumAmount = allPolicies.stream()
                .mapToDouble(Policy::getPremiumAmount)
                .sum();

        // Payment Stats (based on Policy paid flag)
        long paidPayments = allPolicies.stream()
                .filter(Policy::isPaid)
                .count();
        long unpaidPayments = allPolicies.stream()
                .filter(p -> !p.isPaid())
                .count();
        long overduePayments = allPolicies.stream()
                .filter(p -> p.getEndDate() != null
                        && p.getEndDate().isBefore(LocalDate.now())
                        && !p.isPaid())
                .count();

        // Charts Data
        List<Map<String, Object>> policiesByType = getPoliciesByType(allPolicies);
        List<Map<String, Object>> paymentsByMonth = getPaymentsByMonth(allPolicies);
        List<Map<String, Object>> recentActivities = getRecentActivities(allPolicies, allUsers);

        // Reminders
        List<Reminder> allReminders = reminderRepository.findAllByDeletedFalse();
        long totalReminders = allReminders.size();
        long pendingReminders = allReminders.stream()
                .filter(r -> !r.isSent() && !r.isDismissed())
                .count();

        return DashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .adminUsers(adminUsers)
                .totalCompanies(totalCompanies)
                .activeCompanies(activeCompanies)
                .totalInsuranceTypes(totalInsuranceTypes)
                .totalInsuranceProviders(totalInsuranceProviders)
                .totalPolicies(totalPolicies)
                .activePolicies(activePolicies)
                .expiredPolicies(expiredPolicies)
                .expiringSoonPolicies(expiringSoonPolicies)
                .totalPremiumAmount(totalPremiumAmount)
                .totalPayments(totalPolicies)
                .paidPayments(paidPayments)
                .unpaidPayments(unpaidPayments)
                .overduePayments(overduePayments)
                .policiesByType(policiesByType)
                .paymentsByMonth(paymentsByMonth)
                .recentActivities(recentActivities)
                .totalReminders(totalReminders)
                .pendingReminders(pendingReminders)
                .build();
    }

    private List<Map<String, Object>> getPoliciesByType(List<Policy> policies) {
        Map<String, Long> typeCount = policies.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getInsuranceType() != null ? p.getInsuranceType().getName() : "Unknown",
                        Collectors.counting()
                ));

        return typeCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .sorted(Comparator.comparing(m -> ((String) m.get("type"))))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getPaymentsByMonth(List<Policy> policies) {
        Map<String, Double> monthlyAmount = policies.stream()
                .filter(p -> p.isPaid() && p.getPaidDate() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getPaidDate().getYear() + "-" +
                                String.format("%02d", p.getPaidDate().getMonthValue()),
                        Collectors.summingDouble(Policy::getPremiumAmount)
                ));

        LocalDate now = LocalDate.now();
        Map<String, Double> completeMonthlyData = new LinkedHashMap<>();

        for (int i = 11; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            String monthLabel = date.getMonth().toString().substring(0, 3) + " " + date.getYear();
            completeMonthlyData.put(monthLabel, monthlyAmount.getOrDefault(monthKey, 0.0));
        }

        return completeMonthlyData.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("month", entry.getKey());
                    map.put("amount", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentActivities(List<Policy> policies, List<User> users) {
        List<Map<String, Object>> activities = new ArrayList<>();

        policies.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .forEach(policy -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "POLICY_CREATED");
                    activity.put("title", "New Policy Added: " + policy.getPolicyNumber());
                    activity.put("description", "Premium: ₹" + policy.getPremiumAmount());
                    activity.put("time", policy.getCreatedAt().toString());
                    activity.put("icon", "fa-file-contract");
                    activity.put("color", "blue");
                    activities.add(activity);
                });

        policies.stream()
                .filter(p -> p.isPaid() && p.getPaidDate() != null)
                .sorted((a, b) -> b.getPaidDate().compareTo(a.getPaidDate()))
                .limit(5)
                .forEach(policy -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "PAYMENT_RECEIVED");
                    activity.put("title", "Payment Received for Policy: " + policy.getPolicyNumber());
                    activity.put("description", "Amount: ₹" + policy.getPremiumAmount());
                    activity.put("time", policy.getPaidDate().toString());
                    activity.put("icon", "fa-rupee-sign");
                    activity.put("color", "green");
                    activities.add(activity);
                });

        users.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .forEach(user -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "USER_REGISTERED");
                    activity.put("title", "New User Registered: " + user.getName());
                    activity.put("description", "Email: " + user.getEmail());
                    activity.put("time", user.getCreatedAt().toString());
                    activity.put("icon", "fa-user-plus");
                    activity.put("color", "purple");
                    activities.add(activity);
                });

        activities.sort((a, b) -> b.get("time").toString().compareTo(a.get("time").toString()));
        return activities.stream().limit(10).collect(Collectors.toList());
    }
}