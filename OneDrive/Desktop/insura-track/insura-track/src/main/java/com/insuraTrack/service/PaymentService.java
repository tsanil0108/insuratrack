package com.insuraTrack.service;

import com.insuraTrack.dto.PaymentDTO;
import com.insuraTrack.enums.PaymentStatus;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.PremiumPayment;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.repository.PremiumPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PremiumPaymentRepository paymentRepo;
    private final PolicyRepository policyRepo;

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private PaymentDTO toDTO(PremiumPayment pp) {
        return PaymentDTO.builder()
                .id(pp.getId())
                .policyId(pp.getPolicy().getId())
                .policyNumber(pp.getPolicy().getPolicyNumber())
                .amount(pp.getAmount())
                .dueDate(pp.getDueDate())
                .paidDate(pp.getPaidDate())
                .status(pp.getStatus())
                .remarks(pp.getRemarks())
                .build();
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    public List<PaymentDTO> getAll() {
        return paymentRepo.findAllActive()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentDTO> getMyPayments() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return paymentRepo.findAllActive()
                .stream()
                .filter(pp -> pp.getPolicy().getUser() != null
                        && pp.getPolicy().getUser().getEmail().equals(username))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentDTO> getByPolicy(String policyId) {
        return paymentRepo.findByPolicyId(policyId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentDTO> getOverdue() {
        return paymentRepo.findOverdue(LocalDate.now())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentDTO> getUpcoming() {
        LocalDate today = LocalDate.now();
        LocalDate upcoming = today.plusDays(30);
        return paymentRepo.findAllActive()
                .stream()
                .filter(pp -> pp.getStatus() == PaymentStatus.UNPAID
                        && !pp.getDueDate().isBefore(today)
                        && !pp.getDueDate().isAfter(upcoming))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getPaymentSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPaid",    paymentRepo.sumPaidAmount());
        summary.put("totalUnpaid",  paymentRepo.sumUnpaidAmount());
        summary.put("totalOverdue", paymentRepo.sumOverdueAmount());
        summary.put("totalAmount",  paymentRepo.sumTotalAmount());

        List<Map<String, Object>> byProvider = paymentRepo.sumByProvider()
                .stream()
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("provider", row[0]);
                    m.put("total",    row[1]);
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> byCompany = paymentRepo.sumByCompany()
                .stream()
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("company", row[0]);
                    m.put("total",   row[1]);
                    m.put("paid",    row[2]);
                    m.put("unpaid",  row[3]);
                    return m;
                }).collect(Collectors.toList());

        summary.put("byProvider", byProvider);
        summary.put("byCompany",  byCompany);
        return summary;
    }

    // ─── Mutations ───────────────────────────────────────────────────────────

    public PaymentDTO createPayment(String policyId, double amount, LocalDate dueDate) {
        Policy policy = policyRepo.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

        PremiumPayment payment = PremiumPayment.builder()
                .policy(policy)
                .amount(amount)
                .dueDate(dueDate)
                .status(PaymentStatus.UNPAID)
                .build();

        return toDTO(paymentRepo.save(payment));
    }

    public PaymentDTO markAsPaid(String id) {
        PremiumPayment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidDate(LocalDate.now());
        return toDTO(paymentRepo.save(payment));
    }

    public PaymentDTO userMarkAsPaid(String id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PremiumPayment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        String policyOwner = payment.getPolicy().getUser() != null
                ? payment.getPolicy().getUser().getEmail() : null;
        if (!username.equals(policyOwner)) {
            throw new RuntimeException("Access denied");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidDate(LocalDate.now());
        return toDTO(paymentRepo.save(payment));
    }

    public void deletePayment(String id) {
        PremiumPayment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
        payment.setDeleted(true);
        paymentRepo.save(payment);
    }

    public void markOverduePayments() {
        LocalDate today = LocalDate.now();
        List<PremiumPayment> overdue = paymentRepo.findAllActive()
                .stream()
                .filter(pp -> pp.getStatus() == PaymentStatus.UNPAID
                        && pp.getDueDate().isBefore(today))
                .collect(Collectors.toList());

        overdue.forEach(pp -> pp.setStatus(PaymentStatus.OVERDUE));
        paymentRepo.saveAll(overdue);
    }
}