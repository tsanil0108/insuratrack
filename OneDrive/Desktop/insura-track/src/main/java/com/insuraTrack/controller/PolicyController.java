package com.insuraTrack.controller;

import com.insuraTrack.dto.PolicyRequest;
import com.insuraTrack.dto.PolicyResponse;
import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // ── CREATE ─────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> createPolicy(@RequestBody PolicyRequest request) {
        try {
            PolicyResponse response = policyService.createPolicy(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // Handle duplicate policy number
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    @PostMapping("/with-slip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> createPolicyWithSlip(
            @RequestPart("policy") String policyJson,
            @RequestPart(value = "paymentSlip", required = false) MultipartFile slipFile)
            throws Exception {

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        PolicyRequest request = mapper.readValue(policyJson, PolicyRequest.class);

        String slipPath = null;
        if (slipFile != null && !slipFile.isEmpty()) {
            slipPath = savePaymentSlip(slipFile, "new");
        }

        try {
            PolicyResponse response = policyService.createPolicyWithSlip(request, slipPath);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    // ✅ ADD RENEW ENDPOINT
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> renewPolicy(
            @PathVariable String id,
            @RequestBody PolicyRequest request) {
        try {
            PolicyResponse response = policyService.renewPolicy(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .header("X-Error", "Duplicate policy number")
                        .build();
            }
            throw e;
        } catch (IllegalStateException e) {
            // Policy not in EXPIRING_SOON status
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Error", e.getMessage())
                    .build();
        }
    }

    // ✅ ADD RENEW ENDPOINT WITH SLIP UPLOAD
    @PostMapping("/{id}/renew/with-slip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> renewPolicyWithSlip(
            @PathVariable String id,
            @RequestPart("policy") String policyJson,
            @RequestPart(value = "paymentSlip", required = false) MultipartFile slipFile)
            throws Exception {

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        PolicyRequest request = mapper.readValue(policyJson, PolicyRequest.class);

        String slipPath = null;
        if (slipFile != null && !slipFile.isEmpty()) {
            slipPath = savePaymentSlip(slipFile, id + "_renewed");
        }

        // Note: You'll need to add a renewPolicyWithSlip method to your service
        // For now, we'll use the regular renew method
        PolicyResponse response = policyService.renewPolicy(id, request);

        // If slip was uploaded, update the new policy with it
        if (slipPath != null && response != null && response.getId() != null) {
            // Update the newly created policy with the slip
            policyService.updatePolicyWithSlip(response.getId(), request, slipPath);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── READ ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<PolicyResponse> getPolicyById(@PathVariable String id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @GetMapping("/number/{policyNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<PolicyResponse> getPolicyByNumber(@PathVariable String policyNumber) {
        return ResponseEntity.ok(policyService.getPolicyByNumber(policyNumber));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<PolicyResponse>> getPoliciesByUser(@PathVariable String userId) {
        return ResponseEntity.ok(policyService.getPoliciesByUser(userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PolicyResponse>> getPoliciesByStatus(
            @PathVariable PolicyStatus status) {
        return ResponseEntity.ok(policyService.getPoliciesByStatus(status));
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<PolicyResponse>> getPoliciesByCompany(
            @PathVariable String companyId) {
        return ResponseEntity.ok(policyService.getPoliciesByCompany(companyId));
    }

    @GetMapping("/insurance-type/{typeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PolicyResponse>> getPoliciesByType(@PathVariable String typeId) {
        return ResponseEntity.ok(policyService.getPoliciesByInsuranceType(typeId));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PolicyResponse>> getExpiringPolicies(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(policyService.getExpiringPolicies(daysAhead));
    }

    // ✅ ADD ENDPOINT TO CHECK IF POLICY NUMBER EXISTS
    @GetMapping("/check-number/{policyNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Boolean> checkPolicyNumberExists(@PathVariable String policyNumber) {
        try {
            policyService.getPolicyByNumber(policyNumber);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    // ✅ ADD ENDPOINT TO GET RENEWAL HISTORY
    @GetMapping("/{id}/renewal-history")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<PolicyResponse>> getRenewalHistory(@PathVariable String id) {
        // You'll need to add this method to your service
        // For now, return empty list
        return ResponseEntity.ok(List.of());
    }

    // ── EXPIRED EXPORT (CSV) ───────────────────────────────────────────────

    @GetMapping("/expired/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExpiredPoliciesCSV() {
        List<PolicyResponse> expired = policyService.getPoliciesByStatus(PolicyStatus.EXPIRED);

        StringBuilder csv = new StringBuilder();
        csv.append("Policy Number,Company,Provider,Insurance Type,Premium Amount,Start Date,End Date,Status,Description\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (PolicyResponse p : expired) {
            csv.append(escapeCsv(p.getPolicyNumber()))
                    .append(',').append(escapeCsv(p.getCompanyName()))
                    .append(',').append(escapeCsv(p.getProviderName()))
                    .append(',').append(escapeCsv(p.getInsuranceTypeName()))
                    .append(',').append(p.getPremiumAmount())
                    .append(',').append(p.getStartDate() != null ? p.getStartDate().format(fmt) : "")
                    .append(',').append(p.getEndDate() != null ? p.getEndDate().format(fmt) : "")
                    .append(',').append(escapeCsv(p.getStatus()))
                    .append(',').append(escapeCsv(p.getDescription()))
                    .append('\n');
        }

        byte[] bytes = ("\uFEFF" + csv).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"expired_policies_" + LocalDate.now() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable String id,
            @RequestBody PolicyRequest request) {
        return ResponseEntity.ok(policyService.updatePolicy(id, request));
    }

    @PutMapping("/{id}/with-slip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> updatePolicyWithSlip(
            @PathVariable String id,
            @RequestPart("policy") String policyJson,
            @RequestPart(value = "paymentSlip", required = false) MultipartFile slipFile)
            throws Exception {

        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        PolicyRequest request = mapper.readValue(policyJson, PolicyRequest.class);

        String slipPath = null;
        if (slipFile != null && !slipFile.isEmpty()) {
            slipPath = savePaymentSlip(slipFile, id);
        }

        return ResponseEntity.ok(policyService.updatePolicyWithSlip(id, request, slipPath));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> markAsPaid(
            @PathVariable String id,
            @RequestParam double amountPaid,
            @RequestParam(required = false) String paymentReference) {
        return ResponseEntity.ok(policyService.markAsPaid(id, amountPaid, paymentReference));
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable String id,
            @RequestParam String deletedBy) {
        policyService.softDeletePolicy(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    // ── FILE UPLOAD HELPER ─────────────────────────────────────────────────

    private String savePaymentSlip(MultipartFile file, String policyId) throws IOException {
        String uploadDir = System.getProperty("user.home") + "/insuratrack/slips/";
        Files.createDirectories(Paths.get(uploadDir));

        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : "";
        String fileName = "slip_" + policyId + "_"
                + UUID.randomUUID().toString().substring(0, 8) + extension;

        Path targetPath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return "/slips/" + fileName;
    }
}