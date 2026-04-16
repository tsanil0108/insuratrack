package com.insuraTrack.service;

import com.insuraTrack.dto.DashboardResponse;
import com.insuraTrack.enums.PaymentStatus;
import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.enums.Role;
import com.insuraTrack.model.*;
import com.insuraTrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final InsuranceTypeRepository insuranceTypeRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final PolicyRepository policyRepository;
    private final PremiumPaymentRepository premiumPaymentRepository;
    private final ReminderRepository reminderRepository;

    public DashboardResponse getDashboard() {

        // User Stats
        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::isActive).count();
        long adminUsers = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();

        // Company Stats
        List<Company> allCompanies = companyRepository.findAll();
        long totalCompanies = allCompanies.size();
        long activeCompanies = allCompanies.stream().filter(Company::isActive).count();

        // Insurance Stats
        long totalInsuranceTypes = insuranceTypeRepository.count();
        long totalInsuranceProviders = insuranceProviderRepository.count();

        // Policies — use fetch query to avoid lazy-load proxy errors
        List<Policy> allPolicies = policyRepository.findAllWithDetails();
        long totalPolicies = allPolicies.size();
        long activePolicies = allPolicies.stream()
                .filter(p -> p.getStatus() == PolicyStatus.ACTIVE).count();
        long expiredPolicies = allPolicies.stream()
                .filter(p -> p.getStatus() == PolicyStatus.EXPIRED).count();
        long expiringSoonPolicies = allPolicies.stream()
                .filter(p -> p.getStatus() == PolicyStatus.EXPIRING_SOON).count();
        double totalPremiumAmount = allPolicies.stream()
                .mapToDouble(Policy::getPremiumAmount).sum();

        // Payments — use findAllActive() which already has JOIN FETCH
        List<PremiumPayment> allPayments = premiumPaymentRepository.findAllActive();
        long totalPayments = allPayments.size();
        long paidPayments = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID).count();
        long unpaidPayments = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.UNPAID).count();
        long overduePayments = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.OVERDUE).count();

        // Charts
        List<Map<String, Object>> policiesByType = getPoliciesByType(allPolicies);
        List<Map<String, Object>> paymentsByMonth = getPaymentsByMonth(allPayments);

        // Recent Activities
        List<Map<String, Object>> recentActivities = getRecentActivities(allPolicies, allPayments, allUsers);

        // Reminders
        List<Reminder> allReminders = reminderRepository.findAll();
        long totalReminders = allReminders.size();
        long pendingReminders = allReminders.stream()
                .filter(r -> !r.isSent() && !r.isDismissed()).count();

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
                .totalPayments(totalPayments)
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
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getPaymentsByMonth(List<PremiumPayment> payments) {
        Map<String, Double> monthlyAmount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID && p.getPaidDate() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getPaidDate().getYear() + "-" +
                                String.format("%02d", p.getPaidDate().getMonthValue()),
                        Collectors.summingDouble(PremiumPayment::getAmount)
                ));

        return monthlyAmount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("month", entry.getKey());
                    map.put("amount", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentActivities(List<Policy> policies,
                                                          List<PremiumPayment> payments,
                                                          List<User> users) {
        List<Map<String, Object>> activities = new ArrayList<>();

        policies.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .forEach(policy -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "POLICY_CREATED");
                    activity.put("title", "New Policy Added: " + policy.getPolicyNumber());
                    activity.put("time", policy.getCreatedAt().toString());
                    activity.put("icon", "fa-file-contract");
                    activity.put("color", "blue");
                    activities.add(activity);
                });

        payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID && p.getPaidDate() != null)
                .sorted((a, b) -> b.getPaidDate().compareTo(a.getPaidDate()))
                .limit(5)
                .forEach(payment -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "PAYMENT_RECEIVED");
                    activity.put("title", "Payment Received: ₹" + payment.getAmount());
                    activity.put("time", payment.getPaidDate().toString());
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
                    activity.put("time", user.getCreatedAt().toString());
                    activity.put("icon", "fa-user-plus");
                    activity.put("color", "purple");
                    activities.add(activity);
                });

        activities.sort((a, b) -> b.get("time").toString().compareTo(a.get("time").toString()));
        return activities.stream().limit(10).collect(Collectors.toList());
    }
}