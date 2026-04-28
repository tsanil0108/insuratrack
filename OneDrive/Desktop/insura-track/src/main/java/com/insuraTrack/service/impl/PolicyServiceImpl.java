package com.insuraTrack.service.impl;

import com.insuraTrack.dto.PolicyRequest;
import com.insuraTrack.dto.PolicyResponse;
import com.insuraTrack.enums.PaymentMode;
import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.enums.PremiumFrequency;
import com.insuraTrack.exception.ResourceNotFoundException;
import com.insuraTrack.model.*;
import com.insuraTrack.repository.*;
import com.insuraTrack.service.PolicyService;
import com.insuraTrack.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final CompanyRepository companyRepository;
    private final InsuranceTypeRepository insuranceTypeRepository;
    private final InsuranceItemRepository insuranceItemRepository;
    private final InsuranceProviderRepository providerRepository;
    private final HypothecationRepository hypothecationRepository;
    private final ReminderService reminderService;

    // ── CREATE ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PolicyResponse createPolicy(PolicyRequest request) {
        return createPolicyWithSlip(request, null);
    }

    @Override
    @Transactional
    public PolicyResponse createPolicyWithSlip(PolicyRequest request, String slipPath) {
        // ✅ FIX: Check only non-deleted policies
        if (policyRepository.existsByPolicyNumberAndDeletedFalse(request.getPolicyNumber())) {
            throw new IllegalArgumentException(
                    "Policy number already exists: " + request.getPolicyNumber());
        }

        Policy policy = mapToEntity(request, new Policy());

        if (slipPath != null) {
            policy.setPaymentSlipPath(slipPath);
        }

        updatePolicyStatus(policy);
        policy.setPaid(policy.getPremiumAmount() > 0
                && policy.getAmountPaid() >= policy.getPremiumAmount());

        // Ensure deleted flag is false for new policies
        policy.setDeleted(false);
        policy.setDeletedAt(null);
        policy.setDeletedBy(null);

        Policy saved = policyRepository.save(policy);

        try {
            reminderService.generateForPolicy(saved.getId());
        } catch (Exception e) {
            System.err.println("Reminder generation failed (non-fatal): " + e.getMessage());
        }

        return mapToResponse(saved);
    }

    // ── READ ────────────────────────────────────────────────────────────────

    @Override
    public PolicyResponse getPolicyById(String id) {
        return mapToResponse(findPolicyById(id));
    }

    @Override
    public PolicyResponse getPolicyByNumber(String policyNumber) {
        Policy policy = policyRepository.findByPolicyNumberAndDeletedFalse(policyNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found: " + policyNumber));
        return mapToResponse(policy);
    }

    @Override
    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAllByDeletedFalse()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PolicyResponse> getPoliciesByUser(String userId) {
        return policyRepository.findAllByUserIdAndDeletedFalse(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PolicyResponse> getPoliciesByStatus(PolicyStatus status) {
        return policyRepository.findAllByStatusAndDeletedFalse(status)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PolicyResponse> getPoliciesByCompany(String companyId) {
        return policyRepository.findAllByCompanyIdAndDeletedFalse(companyId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PolicyResponse> getPoliciesByInsuranceType(String insuranceTypeId) {
        return policyRepository.findAllByInsuranceTypeIdAndDeletedFalse(insuranceTypeId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<PolicyResponse> getExpiringPolicies(int daysAhead) {
        LocalDate today = LocalDate.now();
        return policyRepository.findExpiringBetween(today, today.plusDays(daysAhead))
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── UPDATE ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PolicyResponse updatePolicy(String id, PolicyRequest request) {
        return updatePolicyWithSlip(id, request, null);
    }

    @Override
    @Transactional
    public PolicyResponse updatePolicyWithSlip(String id, PolicyRequest request, String slipPath) {
        Policy existing = findPolicyById(id);
        LocalDate oldEndDate = existing.getEndDate();

        mapToEntity(request, existing);

        if (slipPath != null) {
            existing.setPaymentSlipPath(slipPath);
        }

        existing.setPaid(existing.getAmountPaid() >= existing.getPremiumAmount()
                && existing.getPremiumAmount() > 0);

        updatePolicyStatus(existing);
        Policy saved = policyRepository.save(existing);

        if (oldEndDate != null && !oldEndDate.equals(request.getEndDate())) {
            try {
                reminderService.dismissAllByPolicy(id);
                reminderService.generateForPolicy(id);
            } catch (Exception e) {
                System.err.println("Reminder regeneration failed (non-fatal): " + e.getMessage());
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PolicyResponse markAsPaid(String id, double amountPaid, String paymentReference) {
        Policy policy = findPolicyById(id);
        policy.setAmountPaid(amountPaid);
        policy.setPaidDate(LocalDate.now());
        policy.setPaymentReference(paymentReference);
        policy.setPaid(policy.isFullyPaid());
        return mapToResponse(policyRepository.save(policy));
    }

    // ── RENEW POLICY ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PolicyResponse renewPolicy(String oldPolicyId, PolicyRequest request) {
        // 1. Get the old policy (must exist and not be deleted)
        Policy oldPolicy = findPolicyById(oldPolicyId);

        // 2. Verify old policy is in EXPIRING_SOON status
        if (oldPolicy.getStatus() != PolicyStatus.EXPIRING_SOON) {
            throw new IllegalStateException(
                    "Can only renew policies that are expiring soon. Current status: " +
                            oldPolicy.getStatus());
        }

        // 3. Check if new policy number already exists (ignoring soft-deleted)
        if (policyRepository.existsByPolicyNumberAndDeletedFalse(request.getPolicyNumber())) {
            throw new IllegalArgumentException(
                    "Policy number already exists: " + request.getPolicyNumber());
        }

        // 4. Create the new policy (copy from old policy + new values)
        Policy newPolicy = new Policy();

        // Copy locked fields from old policy (Company, Insurance Type, Insurance Item)
        newPolicy.setCompany(oldPolicy.getCompany());
        newPolicy.setInsuranceType(oldPolicy.getInsuranceType());
        newPolicy.setInsuranceItem(oldPolicy.getInsuranceItem());
        newPolicy.setProvider(oldPolicy.getProvider());
        newPolicy.setHypothecation(oldPolicy.getHypothecation());
        newPolicy.setUser(oldPolicy.getUser()); // Keep same user if exists

        // Set values from request
        newPolicy.setPolicyNumber(request.getPolicyNumber());
        newPolicy.setDescription(request.getDescription() != null ? request.getDescription() : oldPolicy.getDescription());
        newPolicy.setPremiumAmount(request.getPremiumAmount());
        newPolicy.setSumInsured(request.getSumInsured() > 0 ? request.getSumInsured() : oldPolicy.getSumInsured());
        newPolicy.setStartDate(request.getStartDate());
        newPolicy.setEndDate(request.getEndDate());

        // Set premium frequency
        if (request.getPremiumFrequency() != null && !request.getPremiumFrequency().isBlank()) {
            newPolicy.setPremiumFrequency(PremiumFrequency.valueOf(request.getPremiumFrequency()));
        } else {
            newPolicy.setPremiumFrequency(oldPolicy.getPremiumFrequency());
        }

        // Set payment details
        newPolicy.setAmountPaid(request.getAmountPaid());
        newPolicy.setPaidDate(request.getPaidDate());

        if (request.getPaymentMode() != null && !request.getPaymentMode().isBlank()) {
            newPolicy.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode()));
        }

        newPolicy.setPaymentReference(request.getPaymentReference());

        // Track renewal source
        newPolicy.setRenewedFromPolicyId(oldPolicyId);

        // Set flags
        newPolicy.setDeleted(false);
        newPolicy.setDeletedAt(null);
        newPolicy.setDeletedBy(null);
        newPolicy.setPaid(newPolicy.getAmountPaid() >= newPolicy.getPremiumAmount()
                && newPolicy.getPremiumAmount() > 0);

        // Update status based on dates
        updatePolicyStatus(newPolicy);

        Policy savedNewPolicy = policyRepository.save(newPolicy);

        // 5. Mark old policy as EXPIRED (not deleted!)
        oldPolicy.setStatus(PolicyStatus.EXPIRED);
        oldPolicy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(oldPolicy);

        // 6. Handle reminders - dismiss old, generate for new
        try {
            reminderService.dismissAllByPolicy(oldPolicyId);
            reminderService.generateForPolicy(savedNewPolicy.getId());
        } catch (Exception e) {
            System.err.println("Reminder handling failed (non-fatal): " + e.getMessage());
        }

        return mapToResponse(savedNewPolicy);
    }

    // ── DELETE ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void softDeletePolicy(String id, String deletedByEmail) {
        Policy policy = findPolicyById(id);
        policy.softDelete(deletedByEmail);
        policyRepository.save(policy);

        try {
            reminderService.dismissAllByPolicy(id);
        } catch (Exception e) {
            System.err.println("Reminder dismissal failed (non-fatal): " + e.getMessage());
        }
    }

    @Override
    public List<PolicyResponse> getRenewalHistory(String policyId) {
        // Find all policies that were renewed from this policy
        List<Policy> renewals = policyRepository.findByRenewedFromPolicyId(policyId);
        return renewals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    // ── PRIVATE HELPERS ─────────────────────────────────────────────────────

    private Policy findPolicyById(String id) {
        return policyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found with id: " + id));
    }

    private Policy mapToEntity(PolicyRequest request, Policy policy) {

        policy.setCompany(companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found: " + request.getCompanyId())));

        policy.setInsuranceType(insuranceTypeRepository.findById(request.getInsuranceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Insurance type not found: " + request.getInsuranceTypeId())));

        policy.setProvider(providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found: " + request.getProviderId())));

        if (request.getInsuranceItemId() != null && !request.getInsuranceItemId().isBlank()) {
            policy.setInsuranceItem(insuranceItemRepository.findById(request.getInsuranceItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Insurance item not found: " + request.getInsuranceItemId())));
        } else {
            policy.setInsuranceItem(null);
        }

        if (request.getHypothecationId() != null && !request.getHypothecationId().isBlank()) {
            policy.setHypothecation(hypothecationRepository.findById(request.getHypothecationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Hypothecation not found: " + request.getHypothecationId())));
        } else {
            policy.setHypothecation(null);
        }

        policy.setPolicyNumber(request.getPolicyNumber());
        policy.setDescription(request.getDescription());
        policy.setPremiumAmount(request.getPremiumAmount());
        policy.setSumInsured(request.getSumInsured());
        policy.setStartDate(request.getStartDate());
        policy.setEndDate(request.getEndDate());

        if (request.getPremiumFrequency() != null && !request.getPremiumFrequency().isBlank()) {
            policy.setPremiumFrequency(PremiumFrequency.valueOf(request.getPremiumFrequency()));
        }

        policy.setAmountPaid(request.getAmountPaid());

        if (request.getPaidDate() != null) {
            policy.setPaidDate(request.getPaidDate());
        }

        if (request.getPaymentMode() != null && !request.getPaymentMode().isBlank()) {
            policy.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode()));
        } else {
            policy.setPaymentMode(null);
        }

        if (request.getPaymentReference() != null && !request.getPaymentReference().isBlank()) {
            policy.setPaymentReference(request.getPaymentReference());
        } else {
            policy.setPaymentReference(null);
        }

        policy.setPaid(request.getPremiumAmount() > 0
                && request.getAmountPaid() >= request.getPremiumAmount());

        return policy;
    }

    private PolicyResponse mapToResponse(Policy policy) {
        PolicyResponse r = new PolicyResponse();

        r.setId(policy.getId());
        r.setPolicyNumber(policy.getPolicyNumber());
        r.setDescription(policy.getDescription());
        r.setPremiumAmount(policy.getPremiumAmount());
        r.setSumInsured(policy.getSumInsured());
        r.setStartDate(policy.getStartDate());
        r.setEndDate(policy.getEndDate());
        r.setStatus(policy.getStatus() != null ? policy.getStatus().name() : null);
        r.setPremiumFrequency(policy.getPremiumFrequency() != null
                ? policy.getPremiumFrequency().name() : null);
        r.setCreatedAt(policy.getCreatedAt());
        r.setUpdatedAt(policy.getUpdatedAt());

        r.setAmountPaid(policy.getAmountPaid());
        r.setPaid(policy.isPaid());
        if (policy.getPaidDate() != null)
            r.setPaidDate(policy.getPaidDate());
        if (policy.getPaymentMode() != null)
            r.setPaymentMode(policy.getPaymentMode().name());
        if (policy.getPaymentReference() != null)
            r.setPaymentReference(policy.getPaymentReference());

        if (policy.getCompany() != null) {
            r.setCompanyId(policy.getCompany().getId());
            r.setCompanyName(policy.getCompany().getName());
        }
        if (policy.getInsuranceType() != null) {
            r.setInsuranceTypeId(policy.getInsuranceType().getId());
            r.setInsuranceTypeName(policy.getInsuranceType().getName());
        }
        if (policy.getInsuranceItem() != null) {
            r.setInsuranceItemId(policy.getInsuranceItem().getId());
            r.setInsuranceItemName(policy.getInsuranceItem().getName());
        }
        if (policy.getProvider() != null) {
            r.setProviderId(policy.getProvider().getId());
            r.setProviderName(policy.getProvider().getName());
        }
        if (policy.getUser() != null) {
            r.setUserId(policy.getUser().getId());
            r.setUserName(policy.getUser().getName());
        }
        if (policy.getHypothecation() != null) {
            r.setHypothecationId(policy.getHypothecation().getId());
            r.setHypothecationName(policy.getHypothecation().getBankName());
            r.setHypothecation(true);
        }

        // Add renewal info
        if (policy.getRenewedFromPolicyId() != null) {
            r.setRenewedFromPolicyId(policy.getRenewedFromPolicyId());
        }

        return r;
    }

    private void updatePolicyStatus(Policy policy) {
        LocalDate today = LocalDate.now();
        if (policy.getEndDate() == null) {
            policy.setStatus(PolicyStatus.ACTIVE);
            return;
        }
        if (policy.getEndDate().isBefore(today)) {
            policy.setStatus(PolicyStatus.EXPIRED);
        } else if (policy.getEndDate().isBefore(today.plusDays(30))) {
            policy.setStatus(PolicyStatus.EXPIRING_SOON);
        } else {
            policy.setStatus(PolicyStatus.ACTIVE);
        }
    }
}