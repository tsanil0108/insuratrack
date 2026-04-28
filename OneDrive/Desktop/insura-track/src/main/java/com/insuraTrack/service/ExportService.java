package com.insuraTrack.service;

import com.insuraTrack.model.Policy;
import com.insuraTrack.model.User;
import com.insuraTrack.repository.PolicyRepository;
import com.insuraTrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;

    // ─── CURRENT USER ────────────────────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isAdmin(User user) {
        return user.getRole().name().equalsIgnoreCase("ADMIN");
    }

    // ─── ROLE-BASED FILTERS ──────────────────────────────────────────────────
    private List<Policy> getPoliciesForCurrentUser() {
        User user = getCurrentUser();
        if (isAdmin(user)) {
            return policyRepository.findAllByDeletedFalse();
        }
        return policyRepository.findAllByUserIdAndDeletedFalse(user.getId());
    }

    // ─── POLICIES CSV ────────────────────────────────────────────────────────
    public byte[] exportPoliciesCsv() {
        List<Policy> policies = getPoliciesForCurrentUser();
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("Policy No,Company,Insurance Type,Insurance Item,Provider,User,Premium Amount,Sum Insured,Start Date,End Date,Status,Paid,Frequency\n");

        // Data rows
        for (Policy p : policies) {
            sb.append(csv(p.getPolicyNumber())).append(",")
                    .append(csv(p.getCompany() != null ? p.getCompany().getName() : "")).append(",")
                    .append(csv(p.getInsuranceType() != null ? p.getInsuranceType().getName() : "")).append(",")
                    .append(csv(p.getInsuranceItem() != null ? p.getInsuranceItem().getName() : "")).append(",")
                    .append(csv(p.getProvider() != null ? p.getProvider().getName() : "")).append(",")
                    .append(csv(p.getUser() != null ? p.getUser().getName() : "")).append(",")
                    .append(p.getPremiumAmount()).append(",")
                    .append(p.getSumInsured()).append(",")
                    .append(p.getStartDate()).append(",")
                    .append(p.getEndDate()).append(",")
                    .append(p.getStatus()).append(",")

                    .append(p.getPremiumFrequency()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ─── POLICIES EXCEL ──────────────────────────────────────────────────────
    public byte[] exportPoliciesExcel() throws IOException {
        List<Policy> policies = getPoliciesForCurrentUser();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Policies");

            String[] headers = {
                    "Policy No", "Company", "Insurance Type", "Insurance Item", "Provider", "User",
                    "Premium Amount", "Sum Insured", "Start Date", "End Date", "Status", "Paid", "Frequency"
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
                row.createCell(1).setCellValue(p.getCompany() != null ? p.getCompany().getName() : "");
                row.createCell(2).setCellValue(p.getInsuranceType() != null ? p.getInsuranceType().getName() : "");
                row.createCell(3).setCellValue(p.getInsuranceItem() != null ? p.getInsuranceItem().getName() : "");
                row.createCell(4).setCellValue(p.getProvider() != null ? p.getProvider().getName() : "");
                row.createCell(5).setCellValue(p.getUser() != null ? p.getUser().getName() : "");
                row.createCell(6).setCellValue(p.getPremiumAmount());
                row.createCell(7).setCellValue(p.getSumInsured());
                row.createCell(8).setCellValue(p.getStartDate().toString());
                row.createCell(9).setCellValue(p.getEndDate().toString());
                row.createCell(10).setCellValue(p.getStatus().name());

                row.createCell(12).setCellValue(p.getPremiumFrequency().name());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─── POLICIES PDF (HTML) ─────────────────────────────────────────────────
    public byte[] exportPoliciesPdf() throws IOException {
        List<Policy> policies = getPoliciesForCurrentUser();
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>")
                .append("<html><head><meta charset='UTF-8'><title>Policies Report</title>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; margin: 20px; }")
                .append("h2 { color: #4f46e5; }")
                .append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }")
                .append("th { background-color: #4f46e5; color: white; padding: 10px; text-align: left; }")
                .append("td { border: 1px solid #ddd; padding: 8px; }")
                .append("tr:nth-child(even) { background-color: #f9f9f9; }")
                .append(".status-active { color: green; font-weight: bold; }")
                .append(".status-expired { color: red; font-weight: bold; }")
                .append(".status-expiring { color: orange; font-weight: bold; }")
                .append("</style></head><body>")
                .append("<h2>📋 Policies Report</h2>")
                .append("<p>Generated on: ").append(java.time.LocalDateTime.now()).append("</p>")
                .append("<p>Total Policies: <strong>").append(policies.size()).append("</strong></p>")
                .append("<table>")
                .append("<tr>")
                .append("<th>Policy No</th><th>Company</th><th>Type</th><th>Provider</th>")
                .append("<th>Premium (₹)</th><th>Status</th><th>Paid</th><th>Expiry Date</th>")
                .append("</tr>");

        for (Policy p : policies) {
            String statusClass = "";
            if (p.getStatus().name().equals("ACTIVE")) statusClass = "status-active";
            else if (p.getStatus().name().equals("EXPIRED")) statusClass = "status-expired";
            else statusClass = "status-expiring";

            html.append("<tr>")
                    .append("<td>").append(p.getPolicyNumber()).append("</td>")
                    .append("<td>").append(p.getCompany() != null ? p.getCompany().getName() : "").append("</td>")
                    .append("<td>").append(p.getInsuranceType() != null ? p.getInsuranceType().getName() : "").append("</td>")
                    .append("<td>").append(p.getProvider() != null ? p.getProvider().getName() : "").append("</td>")
                    .append("<td>").append(String.format("%,.2f", p.getPremiumAmount())).append("</td>")
                    .append("<td class='").append(statusClass).append("'>").append(p.getStatus()).append("</td>")

                    .append("<td>").append(p.getEndDate()).append("</td>")
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