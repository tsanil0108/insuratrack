package com.insuraTrack.controller;

import com.insuraTrack.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {

    private final ExportService exportService;

    // ─── Policies ────────────────────────────────────────────
    @GetMapping("/policies/csv")
    public ResponseEntity<byte[]> policiesCsv() throws Exception {
        byte[] data = exportService.exportPoliciesCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=policies.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/policies/excel")
    public ResponseEntity<byte[]> policiesExcel() throws Exception {
        byte[] data = exportService.exportPoliciesExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=policies.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/policies/pdf")
    public ResponseEntity<byte[]> policiesPdf() throws Exception {
        byte[] data = exportService.exportPoliciesPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=policies.html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
    }

    // ─── Payments ────────────────────────────────────────────
    @GetMapping("/payments/csv")
    public ResponseEntity<byte[]> paymentsCsv() throws Exception {
        byte[] data = exportService.exportPaymentsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/payments/excel")
    public ResponseEntity<byte[]> paymentsExcel() throws Exception {
        byte[] data = exportService.exportPaymentsExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/payments/pdf")
    public ResponseEntity<byte[]> paymentsPdf() throws Exception {
        byte[] data = exportService.exportPaymentsPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments.html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
    }
}