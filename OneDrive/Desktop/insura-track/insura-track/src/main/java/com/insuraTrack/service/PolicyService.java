package com.insuraTrack.service;

import com.insuraTrack.dto.PolicyRequest;
import com.insuraTrack.dto.PolicyResponse;
import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.model.*;
import com.insuraTrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CompanyRepository companyRepository;
    private final InsuranceTypeRepository insuranceTypeRepository;
    private final InsuranceProviderRepository providerRepository;
    private final UserRepository userRepository;

    // ─── CURRENT USER ────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── ADMIN ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PolicyResponse> getAll() {
        return policyRepository.findAllActive()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getByCompany(String companyId) {
        return policyRepository.findByCompanyIdAndDeletedFalse(companyId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ─── USER ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PolicyResponse> getMyPolicies() {
        User user = getCurrentUser();
        return policyRepository.findByUserAndDeletedFalse(user)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ─── SECURE GET ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PolicyResponse getByIdSecure(String id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found"));

        User currentUser = getCurrentUser();

        if (!currentUser.getRole().name().equals("ADMIN")) {
            if (policy.getUser() == null ||
                    !policy.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized access");
            }
        }

        return convertToResponse(policy);
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Transactional
    public PolicyResponse create(PolicyRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        InsuranceType type = insuranceTypeRepository.findById(request.getInsuranceTypeId())
                .orElseThrow(() -> new RuntimeException("Insurance type not found"));

        InsuranceProvider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        // If admin provided a userId, assign that user; otherwise assign current user
        User user;
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));
        } else {
            user = getCurrentUser();
        }

        Policy policy = Policy.builder()
                .policyNumber(request.getPolicyNumber())
                .company(company)
                .insuranceType(type)
                .provider(provider)
                .user(user)
                .description(request.getDescription())
                .premiumAmount(request.getPremiumAmount())
                .sumInsured(request.getSumInsured())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .premiumFrequency(request.getPremiumFrequency())
                .hypothecation(request.isHypothecation())
                .status(PolicyStatus.ACTIVE)
                .build();

        return convertToResponse(policyRepository.save(policy));
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Transactional
    public PolicyResponse update(String id, PolicyRequest request) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found"));

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        InsuranceType type = insuranceTypeRepository.findById(request.getInsuranceTypeId())
                .orElseThrow(() -> new RuntimeException("Insurance type not found"));

        InsuranceProvider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        policy.setPolicyNumber(request.getPolicyNumber());
        policy.setCompany(company);
        policy.setInsuranceType(type);
        policy.setProvider(provider);
        policy.setDescription(request.getDescription());
        policy.setPremiumAmount(request.getPremiumAmount());
        policy.setSumInsured(request.getSumInsured());
        policy.setStartDate(request.getStartDate());
        policy.setEndDate(request.getEndDate());
        policy.setPremiumFrequency(request.getPremiumFrequency());
        policy.setHypothecation(request.isHypothecation());

        // Allow admin to reassign user
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));
            policy.setUser(user);
        }

        return convertToResponse(policyRepository.save(policy));
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(String id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        policy.softDelete("admin");
        policyRepository.save(policy);
    }

    // ─── AUTO STATUS ─────────────────────────────────────────────────────────

    @Transactional
    public void updateExpiredPolicies() {
        LocalDate today = LocalDate.now();
        policyRepository.findExpired(today, PolicyStatus.ACTIVE)
                .forEach(p -> {
                    p.setStatus(PolicyStatus.EXPIRED);
                    policyRepository.save(p);
                });
    }

    @Transactional
    public void updateExpiringSoonStatus() {
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(30);
        policyRepository.findExpiringSoon(today, soon)
                .forEach(p -> {
                    if (p.getStatus() == PolicyStatus.ACTIVE) {
                        p.setStatus(PolicyStatus.EXPIRING_SOON);
                        policyRepository.save(p);
                    }
                });
    }

    // ─── DTO ─────────────────────────────────────────────────────────────────

    private PolicyResponse convertToResponse(Policy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .companyId(policy.getCompany().getId())
                .companyName(policy.getCompany().getName())
                .insuranceTypeId(policy.getInsuranceType().getId())
                .insuranceTypeName(policy.getInsuranceType().getName())
                .providerId(policy.getProvider().getId())
                .providerName(policy.getProvider().getName())
                .userId(policy.getUser() != null ? policy.getUser().getId() : null)
                .userName(policy.getUser() != null ? policy.getUser().getName() : null)
                .description(policy.getDescription())
                .premiumAmount(policy.getPremiumAmount())
                .sumInsured(policy.getSumInsured())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .status(policy.getStatus().name())
                .premiumFrequency(policy.getPremiumFrequency().name())
                .hypothecation(policy.isHypothecation())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}