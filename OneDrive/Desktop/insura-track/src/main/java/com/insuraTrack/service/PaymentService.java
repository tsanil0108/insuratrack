package com.insuraTrack.service;

import com.insuraTrack.dto.PaymentDTO;
import com.insuraTrack.enums.PaymentStatus;
import com.insuraTrack.model.Policy;
import com.insuraTrack.model.PremiumPayment;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.repository.PremiumPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PremiumPaymentRepository paymentRepo;
    private final PolicyRepository policyRepo;

    @Value("${payment.slip.upload.dir:uploads/payment-slips}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        // Get the project root directory
        String projectRoot = System.getProperty("user.dir");

        // Resolve the upload path
        this.uploadPath = Paths.get(projectRoot).resolve(uploadDir).toAbsolutePath();

        try {
            // Create directory if it doesn't exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("✅ Created upload directory: " + uploadPath);
            }
            System.out.println("✅ Upload directory ready: " + uploadPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to create upload directory: " + e.getMessage());
            throw new RuntimeException("Could not initialize upload directory", e);
        }
    }

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
                .paymentSlipUrl(pp.getPaymentSlipUrl())
                .paymentReference(pp.getPaymentReference())
                .paymentMethod(pp.getPaymentMethod())
                .paymentVerifiedDate(pp.getPaymentVerifiedDate())
                .adminRemarks(pp.getAdminRemarks())
                .build();
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PaymentDTO> getAll() {
        return paymentRepo.findAllActive()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getMyPayments() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return paymentRepo.findAllActive()
                .stream()
                .filter(pp -> pp.getPolicy().getUser() != null
                        && pp.getPolicy().getUser().getEmail().equals(username))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getPendingVerification() {
        return paymentRepo.findAllActive()
                .stream()
                .filter(pp -> pp.getStatus() == PaymentStatus.PENDING_VERIFICATION)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getByPolicy(String policyId) {
        return paymentRepo.findByPolicyId(policyId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getOverdue() {
        return paymentRepo.findOverdue(LocalDate.now())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPaid", paymentRepo.sumPaidAmount());
        summary.put("totalUnpaid", paymentRepo.sumUnpaidAmount());
        summary.put("totalOverdue", paymentRepo.sumOverdueAmount());
        summary.put("totalAmount", paymentRepo.sumTotalAmount());
        summary.put("pendingVerification", paymentRepo.countPendingVerification());

        List<Map<String, Object>> byProvider = paymentRepo.sumByProvider()
                .stream()
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("provider", row[0]);
                    m.put("total", row[1]);
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> byCompany = paymentRepo.sumByCompany()
                .stream()
                .map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("company", row[0]);
                    m.put("total", row[1]);
                    m.put("paid", row[2]);
                    m.put("unpaid", row[3]);
                    return m;
                }).collect(Collectors.toList());

        summary.put("byProvider", byProvider);
        summary.put("byCompany", byCompany);
        return summary;
    }

    // ─── Mutations ───────────────────────────────────────────────────────────

    @Transactional
    public PaymentDTO createPayment(String policyId, double amount, LocalDate dueDate) {
        Policy policy = policyRepo.findActiveById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

        PremiumPayment payment = PremiumPayment.builder()
                .policy(policy)
                .amount(amount)
                .dueDate(dueDate)
                .status(PaymentStatus.UNPAID)
                .build();

        return toDTO(paymentRepo.save(payment));
    }

    @Transactional
    public PaymentDTO uploadPaymentSlip(String id, MultipartFile slip, String reference, String paymentMethod) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        PremiumPayment payment = paymentRepo.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        String policyOwner = payment.getPolicy().getUser() != null
                ? payment.getPolicy().getUser().getEmail() : null;

        if (!username.equals(policyOwner)) {
            throw new RuntimeException("Access denied");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Payment already processed");
        }

        try {
            // Ensure directory exists
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = slip.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(filename);

            // Save file
            slip.transferTo(filePath.toFile());
            System.out.println("✅ File saved: " + filePath.toAbsolutePath());

            // Update payment record
            payment.setPaymentSlipUrl(filename);
            if (reference != null && !reference.isEmpty()) {
                payment.setPaymentReference(reference);
            }
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                payment.setPaymentMethod(paymentMethod);
            }
            payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
            payment.setRemarks("Payment slip uploaded, awaiting verification");

            return toDTO(paymentRepo.save(payment));

        } catch (IOException e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload payment slip: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentDTO verifyPayment(String id, String adminRemarks) {
        PremiumPayment payment = paymentRepo.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        if (payment.getStatus() != PaymentStatus.PENDING_VERIFICATION) {
            throw new RuntimeException("Payment is not pending verification");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidDate(LocalDate.now());
        payment.setPaymentVerifiedDate(LocalDate.now());
        payment.setAdminRemarks(adminRemarks);
        payment.setRemarks("Payment verified by admin");

        return toDTO(paymentRepo.save(payment));
    }

    @Transactional
    public PaymentDTO rejectPayment(String id, String rejectionReason) {
        PremiumPayment payment = paymentRepo.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        if (payment.getStatus() != PaymentStatus.PENDING_VERIFICATION) {
            throw new RuntimeException("Payment is not pending verification");
        }

        payment.setStatus(PaymentStatus.UNPAID);
        payment.setAdminRemarks(rejectionReason);
        payment.setRemarks("Payment rejected: " + rejectionReason);
        payment.setPaymentSlipUrl(null); // Clear the slip URL

        return toDTO(paymentRepo.save(payment));
    }

    @Transactional
    public ResponseEntity<byte[]> getPaymentSlip(String id) {
        PremiumPayment payment = paymentRepo.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String policyOwner = payment.getPolicy().getUser() != null
                ? payment.getPolicy().getUser().getEmail() : null;

        if (!isAdmin && !username.equals(policyOwner)) {
            throw new RuntimeException("Access denied");
        }

        if (payment.getPaymentSlipUrl() == null) {
            throw new RuntimeException("No payment slip found");
        }

        try {
            Path filePath = uploadPath.resolve(payment.getPaymentSlipUrl());
            System.out.println("📄 Looking for file: " + filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                throw new RuntimeException("Payment slip file not found at: " + filePath);
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            byte[] fileBytes = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payment.getPaymentSlipUrl() + "\"")
                    .body(fileBytes);

        } catch (IOException e) {
            System.err.println("❌ Failed to retrieve slip: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve payment slip: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentDTO markAsPaid(String id) {
        PremiumPayment payment = paymentRepo.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidDate(LocalDate.now());
        return toDTO(paymentRepo.save(payment));
    }

    @Transactional
    public PaymentDTO userMarkAsPaid(String id) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        PremiumPayment payment = paymentRepo.findActiveById(id)
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

    @Transactional
    public void deletePayment(String id) {
        PremiumPayment payment = paymentRepo.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
        payment.softDelete(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        paymentRepo.save(payment);
    }

    @Transactional
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