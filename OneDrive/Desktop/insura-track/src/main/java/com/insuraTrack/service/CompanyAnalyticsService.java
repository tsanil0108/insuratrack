package com.insuraTrack.service;

import com.insuraTrack.model.Policy;
import com.insuraTrack.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyAnalyticsService {

    private final PolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getCompanyAnalytics(String companyId) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Policy> policies = policyRepository.findAllByCompanyIdAndDeletedFalse(companyId);

        result.put("totalPolicies", policies.size());

        // ── 1. Status Breakdown ───────────────────────────────────────────────
        Map<String, Long> statusBreakdown = policies.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus() != null ? p.getStatus().name() : "UNKNOWN",
                        Collectors.counting()
                ));
        result.put("policyStatusBreakdown", statusBreakdown);

        // ── 2. Insurance Type Breakdown ───────────────────────────────────────
        Map<String, Long> typeBreakdown = policies.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getInsuranceType() != null && p.getInsuranceType().getName() != null
                                ? p.getInsuranceType().getName() : "Unknown",
                        Collectors.counting()
                ));
        result.put("insuranceTypeBreakdown", typeBreakdown);

        // ── 3. Payment Summary (handles partial payments correctly) ───────────
        double totalPremium = policies.stream().mapToDouble(Policy::getPremiumAmount).sum();

        // ✅ FIX: Use amountPaid for partial payments, not just isPaid flag
        double totalPaid = policies.stream()
                .mapToDouble(p -> p.getAmountPaid() > 0 ? p.getAmountPaid() : 0)
                .sum();
        double totalRemaining = Math.max(0, totalPremium - totalPaid);

        long paidCount = policies.stream()
                .filter(p -> p.isPaid() || (p.getPremiumAmount() > 0 && p.getAmountPaid() >= p.getPremiumAmount()))
                .count();
        long partialCount = policies.stream()
                .filter(p -> !p.isPaid() && p.getAmountPaid() > 0 && p.getAmountPaid() < p.getPremiumAmount())
                .count();
        long unpaidCount = policies.stream()
                .filter(p -> p.getAmountPaid() == 0)
                .count();

        Map<String, Object> paymentSummary = new LinkedHashMap<>();
        paymentSummary.put("totalPremium", totalPremium);
        paymentSummary.put("totalPaid", totalPaid);
        paymentSummary.put("totalRemaining", totalRemaining);
        paymentSummary.put("paidCount", paidCount);
        paymentSummary.put("partialCount", partialCount);
        paymentSummary.put("unpaidCount", unpaidCount);
        paymentSummary.put("collectionRate",
                totalPremium > 0 ? Math.round((totalPaid / totalPremium) * 100) : 0);
        result.put("paymentSummary", paymentSummary);

        // Payment status breakdown (3-way: Paid / Partial / Unpaid)
        Map<String, Long> paymentStatusBreakdown = new LinkedHashMap<>();
        paymentStatusBreakdown.put("PAID", paidCount);
        paymentStatusBreakdown.put("PARTIAL", partialCount);
        paymentStatusBreakdown.put("UNPAID", unpaidCount);
        result.put("paymentStatusBreakdown", paymentStatusBreakdown);

        // ── 4. Top-10 Policies by Premium ────────────────────────────────────
        List<Map<String, Object>> premiumByPolicy = policies.stream()
                .sorted(Comparator.comparing(Policy::getPremiumAmount).reversed())
                .limit(10)
                .map(p -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("policyNumber", p.getPolicyNumber());
                    entry.put("type", p.getInsuranceType() != null ? p.getInsuranceType().getName() : "Unknown");
                    entry.put("premiumAmount", p.getPremiumAmount());
                    entry.put("amountPaid", p.getAmountPaid());
                    entry.put("remaining", Math.max(0, p.getPremiumAmount() - p.getAmountPaid()));
                    entry.put("status", p.getStatus() != null ? p.getStatus().name() : "UNKNOWN");
                    entry.put("paid", p.isPaid());
                    entry.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
                    entry.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
                    return entry;
                })
                .collect(Collectors.toList());
        result.put("premiumByPolicy", premiumByPolicy);

        // ── 5. Year-wise Breakdown (ALL years, not just last 12 months) ───────
        // ✅ FIX: Group by END DATE year, show premium vs paid per year
        Map<String, Map<String, Double>> yearMap = new LinkedHashMap<>();

        for (Policy p : policies) {
            if (p.getEndDate() == null) continue;
            String year = String.valueOf(p.getEndDate().getYear());
            yearMap.computeIfAbsent(year, k -> new LinkedHashMap<>());
            yearMap.get(year).merge("premium", p.getPremiumAmount(), Double::sum);
            yearMap.get(year).merge("paid", p.getAmountPaid(), Double::sum);
            yearMap.get(year).merge("count", 1.0, Double::sum);
        }

        // Sort by year
        List<String> sortedYears = new ArrayList<>(yearMap.keySet());
        Collections.sort(sortedYears);

        List<Map<String, Object>> yearWiseData = sortedYears.stream()
                .map(year -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    Map<String, Double> data = yearMap.get(year);
                    double prem = data.getOrDefault("premium", 0.0);
                    double paid = data.getOrDefault("paid", 0.0);
                    entry.put("year", year);
                    entry.put("premium", prem);
                    entry.put("paid", paid);
                    entry.put("remaining", Math.max(0, prem - paid));
                    entry.put("count", data.getOrDefault("count", 0.0).intValue());
                    entry.put("collectionRate", prem > 0 ? Math.round((paid / prem) * 100) : 0);
                    return entry;
                })
                .collect(Collectors.toList());
        result.put("yearWiseData", yearWiseData);

        // ── 6. Monthly Payment Trend (last 12 months, by paidDate) ───────────
        List<Map<String, Object>> monthlyPaymentTrend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthLabel = month.getMonth().toString().substring(0, 3) + " " + month.getYear();
            final int year = month.getYear();
            final int monthValue = month.getMonthValue();

            // Policies paid in this month
            double paidInMonth = policies.stream()
                    .filter(p -> p.getPaidDate() != null
                            && p.getPaidDate().getYear() == year
                            && p.getPaidDate().getMonthValue() == monthValue)
                    .mapToDouble(Policy::getAmountPaid)
                    .sum();

            // Policies whose end date falls in this month (due/expiring)
            double premiumDueInMonth = policies.stream()
                    .filter(p -> p.getEndDate() != null
                            && p.getEndDate().getYear() == year
                            && p.getEndDate().getMonthValue() == monthValue)
                    .mapToDouble(Policy::getPremiumAmount)
                    .sum();

            Map<String, Object> trend = new LinkedHashMap<>();
            trend.put("month", monthLabel);
            trend.put("paid", paidInMonth);
            trend.put("premiumDue", premiumDueInMonth);
            monthlyPaymentTrend.add(trend);
        }
        result.put("monthlyPaymentTrend", monthlyPaymentTrend);

        // ── 7. Expiry Timeline (next 6 months) ───────────────────────────────
        LocalDate today = LocalDate.now();
        LocalDate sixMonthsLater = today.plusMonths(6);

        List<Map<String, Object>> expiryTimeline = policies.stream()
                .filter(p -> p.getEndDate() != null)
                .filter(p -> !p.getEndDate().isBefore(today) && !p.getEndDate().isAfter(sixMonthsLater))
                .sorted(Comparator.comparing(Policy::getEndDate))
                .map(p -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("policyNumber", p.getPolicyNumber());
                    entry.put("endDate", p.getEndDate().toString());
                    long daysLeft = today.until(p.getEndDate(), java.time.temporal.ChronoUnit.DAYS);
                    entry.put("daysLeft", daysLeft);
                    entry.put("status", p.getStatus() != null ? p.getStatus().name() : "UNKNOWN");
                    entry.put("premiumAmount", p.getPremiumAmount());
                    entry.put("amountPaid", p.getAmountPaid());
                    entry.put("type", p.getInsuranceType() != null ? p.getInsuranceType().getName() : "");
                    return entry;
                })
                .collect(Collectors.toList());
        result.put("expiryTimeline", expiryTimeline);

        return result;
    }
}