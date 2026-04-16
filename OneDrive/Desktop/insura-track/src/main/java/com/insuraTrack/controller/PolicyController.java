package com.insuraTrack.controller;

import com.insuraTrack.dto.PolicyRequest;
import com.insuraTrack.dto.PolicyResponse;
import com.insuraTrack.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // 👑 ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PolicyResponse> getAll() {
        return policyService.getAll();
    }

    // 👤 USER own policies
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public List<PolicyResponse> getMyPolicies() {
        return policyService.getMyPolicies();
    }

    // 👤 USER + ADMIN
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public PolicyResponse getById(@PathVariable String id) {
        return policyService.getByIdSecure(id);
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PolicyResponse> getByCompany(@PathVariable String companyId) {
        return policyService.getByCompany(companyId);
    }

    // 👑 ADMIN only
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> create(@RequestBody PolicyRequest request) {
        return ResponseEntity.ok(policyService.create(request));
    }

    // 👑 ADMIN only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> update(@PathVariable String id,
                                                 @RequestBody PolicyRequest request) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    // 👑 ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        policyService.delete(id);
        return ResponseEntity.ok().build();
    }
}