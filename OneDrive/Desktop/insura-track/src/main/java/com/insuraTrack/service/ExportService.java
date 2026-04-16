package com.insuraTrack.service;

import com.insuraTrack.model.Policy;
import com.insuraTrack.model.PremiumPayment;
import com.insuraTrack.model.User;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.repository.PremiumPaymentRepository;
import com.insuraTrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final PolicyRepository         policyRepository;
    private final PremiumPaymentRepository paymentRepository;
    private final UserRepository           userRepository;

    // ─── CURRENT USER ────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isAdmin(User user) {
        return user.getRole().name().equalsIgnoreCase("ADMIN");
    }

    // ─── ROLE-BASED FILTERS ──────────────────────────────────────────────────

    private List<Policy> getPoliciesForCurrentUser() {
        User user = getCurrentUser();
        return isAdmin(user)
                ? policyRepository.findAllActive()
                : policyRepository.findByUserAndDeletedFalse(user);
    }

    private List<PremiumPayment> getPaymentsForCurrentUser() {
        User user = getCurrentUser();
        if (isAdmin(user)) return paymentRepository.findAllActive();

        // For USER role: only return payments belonging to their own policies
        List<String> policyIds = policyRepository
                .findByUserAndDeletedFalse(user)
                .stream()
                .map(Policy::getId)
                .collect(Collectors.toList());

        return paymentRepository.findAllActive()
                .stream()
                .filter(p -> policyIds.contains(p.getPolicy().getId()))
                .collect(Collectors.toList());
    }

    // ─── POLICIES CSV ────────────────────────────────────────────────────────

    public byte[] exportPoliciesCsv() {
        List<Policy> policies = getPoliciesForCurrentUser();
        StringBuilder sb = new StringBuilder();
        sb.append("Policy No,Company,Insurance Type,Provider,Premium,Sum Insured,Start Date,End Date,Status,Frequency\n");
        for (Policy p : policies) {
            sb.append(csv(p.getPolicyNumber())).append(",")
                    .append(csv(p.getCompany().getName())).append(",")
                    .append(csv(p.getInsuranceType().getName())).append(",")
                    .append(csv(p.getProvider().getName())).append(",")
                    .append(p.getPremiumAmount()).append(",")
                    .append(p.getSumInsured()).append(",")
                    .append(p.getStartDate()).append(",")
                    .append(p.getEndDate()).append(",")
                    .append(p.getStatus()).append(",")
                    .append(p.getPremiumFrequency()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ─── PAYMENTS CSV ────────────────────────────────────────────────────────

    public byte[] exportPaymentsCsv() {
        List<PremiumPayment> payments = getPaymentsForCurrentUser();
        StringBuilder sb = new StringBuilder();
        sb.append("Policy No,Company,Amount,Due Date,Paid Date,Status,Remarks\n");
        for (PremiumPayment p : payments) {
            sb.append(csv(p.getPolicy().getPolicyNumber())).append(",")
                    .append(csv(p.getPolicy().getCompany().getName())).append(",")
                    .append(p.getAmount()).append(",")
                    .append(p.getDueDate()).append(",")
                    .append(p.getPaidDate() != null ? p.getPaidDate() : "").append(",")
                    .append(p.getStatus()).append(",")
                    .append(csv(p.getRemarks())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ─── POLICIES EXCEL ──────────────────────────────────────────────────────

    public byte[] exportPoliciesExcel() throws IOException {
        List<Policy> policies = getPoliciesForCurrentUser();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Policies");

            String[] headers = {
                    "Policy No", "Company", "Insurance Type", "Provider",
                    "Premium", "Sum Insured", "Start Date", "End Date", "Status", "Frequency"
            };
            Row headerRow = sheet.createRow(0);
            CellStyle hs = headerStyle(wb);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(hs);
            }

            int rowNum = 1;
            for (Policy p : policies) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getPolicyNumber());
                row.createCell(1).setCellValue(p.getCompany().getName());
                row.createCell(2).setCellValue(p.getInsuranceType().getName());
                row.createCell(3).setCellValue(p.getProvider().getName());
                row.createCell(4).setCellValue(p.getPremiumAmount());
                row.createCell(5).setCellValue(p.getSumInsured());
                row.createCell(6).setCellValue(p.getStartDate().toString());
                row.createCell(7).setCellValue(p.getEndDate().toString());
                row.createCell(8).setCellValue(p.getStatus().name());
                row.createCell(9).setCellValue(p.getPremiumFrequency().name());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─── PAYMENTS EXCEL ──────────────────────────────────────────────────────

    public byte[] exportPaymentsExcel() throws IOException {
        List<PremiumPayment> payments = getPaymentsForCurrentUser();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Payments");

            String[] headers = {
                    "Policy No", "Company", "Amount", "Due Date", "Paid Date", "Status", "Remarks"
            };
            Row headerRow = sheet.createRow(0);
            CellStyle hs = headerStyle(wb);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(hs);
            }

            int rowNum = 1;
            for (PremiumPayment p : payments) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getPolicy().getPolicyNumber());
                row.createCell(1).setCellValue(p.getPolicy().getCompany().getName());
                row.createCell(2).setCellValue(p.getAmount());
                row.createCell(3).setCellValue(p.getDueDate().toString());
                row.createCell(4).setCellValue(p.getPaidDate() != null ? p.getPaidDate().toString() : "");
                row.createCell(5).setCellValue(p.getStatus().name());
                row.createCell(6).setCellValue(p.getRemarks() != null ? p.getRemarks() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─── POLICIES PDF ────────────────────────────────────────────────────────

    public byte[] exportPoliciesPdf() throws IOException {
        List<Policy> policies = getPoliciesForCurrentUser();
        StringBuilder html = new StringBuilder();
        html.append("<html><body><h2>Policies Report</h2>")
                .append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;width:100%'>")
                .append("<tr style='background:#4f46e5;color:white'>")
                .append("<th>Policy No</th><th>Company</th><th>Type</th><th>Provider</th>")
                .append("<th>Premium</th><th>Status</th><th>Expiry</th></tr>");
        for (Policy p : policies) {
            html.append("<tr>")
                    .append("<td>").append(p.getPolicyNumber()).append("</td>")
                    .append("<td>").append(p.getCompany().getName()).append("</td>")
                    .append("<td>").append(p.getInsuranceType().getName()).append("</td>")
                    .append("<td>").append(p.getProvider().getName()).append("</td>")
                    .append("<td>&#8377;").append(String.format("%.0f", p.getPremiumAmount())).append("</td>")
                    .append("<td>").append(p.getStatus()).append("</td>")
                    .append("<td>").append(p.getEndDate()).append("</td>")
                    .append("</tr>");
        }
        html.append("</table></body></html>");
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ─── PAYMENTS PDF ────────────────────────────────────────────────────────

    public byte[] exportPaymentsPdf() throws IOException {
        List<PremiumPayment> payments = getPaymentsForCurrentUser();
        StringBuilder html = new StringBuilder();
        html.append("<html><body><h2>Payments Report</h2>")
                .append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;width:100%'>")
                .append("<tr style='background:#4f46e5;color:white'>")
                .append("<th>Policy No</th><th>Company</th><th>Amount</th>")
                .append("<th>Due Date</th><th>Paid Date</th><th>Status</th></tr>");
        for (PremiumPayment p : payments) {
            html.append("<tr>")
                    .append("<td>").append(p.getPolicy().getPolicyNumber()).append("</td>")
                    .append("<td>").append(p.getPolicy().getCompany().getName()).append("</td>")
                    .append("<td>&#8377;").append(String.format("%.0f", p.getAmount())).append("</td>")
                    .append("<td>").append(p.getDueDate()).append("</td>")
                    .append("<td>").append(p.getPaidDate() != null ? p.getPaidDate() : "-").append("</td>")
                    .append("<td>").append(p.getStatus()).append("</td>")
                    .append("</tr>");
        }
        html.append("</table></body></html>");
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private String csv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}