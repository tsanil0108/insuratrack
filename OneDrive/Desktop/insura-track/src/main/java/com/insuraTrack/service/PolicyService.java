package com.insuraTrack.service;

import com.insuraTrack.dto.PolicyRequest;
import com.insuraTrack.dto.PolicyResponse;
import com.insuraTrack.enums.PolicyStatus;

import java.util.List;

public interface PolicyService {

    // Create
    PolicyResponse createPolicy(PolicyRequest request);
    PolicyResponse createPolicyWithSlip(PolicyRequest request, String slipPath);

    // Read
    PolicyResponse getPolicyById(String id);
    PolicyResponse getPolicyByNumber(String policyNumber);
    List<PolicyResponse> getAllPolicies();
    List<PolicyResponse> getPoliciesByUser(String userId);
    List<PolicyResponse> getPoliciesByStatus(PolicyStatus status);
    List<PolicyResponse> getPoliciesByCompany(String companyId);
    List<PolicyResponse> getPoliciesByInsuranceType(String insuranceTypeId);
    List<PolicyResponse> getExpiringPolicies(int daysAhead);

    // Update
    PolicyResponse updatePolicy(String id, PolicyRequest request);
    PolicyResponse updatePolicyWithSlip(String id, PolicyRequest request, String slipPath);
    PolicyResponse markAsPaid(String id, double amountPaid, String paymentReference);

    // Renew
    PolicyResponse renewPolicy(String oldPolicyId, PolicyRequest request);

    // ✅ ADD THIS MISSING METHOD
    List<PolicyResponse> getRenewalHistory(String policyId);

    // Delete
    void softDeletePolicy(String id, String deletedByEmail);
}