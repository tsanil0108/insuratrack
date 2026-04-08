package com.insuraTrack.controller;

import com.insuraTrack.dto.PaymentDTO;
import com.insuraTrack.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PaymentDTO>> getMyPayments() {
        return ResponseEntity.ok(paymentService.getMyPayments());
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getByPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(paymentService.getByPolicy(policyId));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getOverdue() {
        return ResponseEntity.ok(paymentService.getOverdue());
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getUpcoming() {
        return ResponseEntity.ok(paymentService.getUpcoming());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getPaymentSummary() {
        return ResponseEntity.ok(paymentService.getPaymentSummary());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDTO> create(@RequestBody Map<String, String> body) {
        String policyId   = body.get("policyId");
        double amount     = Double.parseDouble(body.get("amount"));
        LocalDate dueDate = LocalDate.parse(body.get("dueDate"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(policyId, amount, dueDate));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDTO> markPaid(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.markAsPaid(id));
    }

    @PutMapping("/{id}/user-pay")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentDTO> userMarkPaid(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.userMarkAsPaid(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok().build();
    }
}