package com.insuraTrack.service;

import com.insuraTrack.enums.PaymentStatus;
import com.insuraTrack.enums.PremiumFrequency;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.PremiumPayment;
import com.insuraTrack.repository.PremiumPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentScheduleService {

    private final PremiumPaymentRepository paymentRepository;

    @Transactional
    public void generatePaymentSchedule(Policy policy) {
        if (policy.getStartDate() == null || policy.getEndDate() == null) return;

        List<PremiumPayment> payments = new ArrayList<>();
        LocalDate start = policy.getStartDate();
        LocalDate end   = policy.getEndDate();
        PremiumFrequency freq = policy.getPremiumFrequency();

        int monthsInterval = switch (freq) {
            case MONTHLY     -> 1;
            case QUARTERLY   -> 3;
            case HALF_YEARLY -> 6;
            case ANNUALLY, YEARLY -> 12;
        };

        double installmentAmount = switch (freq) {
            case MONTHLY     -> policy.getPremiumAmount() / 12;
            case QUARTERLY   -> policy.getPremiumAmount() / 4;
            case HALF_YEARLY -> policy.getPremiumAmount() / 2;
            case ANNUALLY, YEARLY -> policy.getPremiumAmount();
        };

        double rounded = Math.round(installmentAmount * 100.0) / 100.0;

        LocalDate dueDate = start;
        while (!dueDate.isAfter(end)) {
            payments.add(PremiumPayment.builder()
                    .policy(policy)
                    .amount(rounded)
                    .dueDate(dueDate)
                    .status(PaymentStatus.UNPAID)
                    .remarks("Auto-generated (" + freq.name().toLowerCase() + ")")
                    .build());
            dueDate = dueDate.plusMonths(monthsInterval);
        }

        paymentRepository.saveAll(payments);
    }

    @Transactional
    public void regeneratePaymentSchedule(Policy policy) {
        List<PremiumPayment> existing = paymentRepository.findByPolicyId(policy.getId());
        paymentRepository.deleteAll(existing);
        generatePaymentSchedule(policy);
    }
}