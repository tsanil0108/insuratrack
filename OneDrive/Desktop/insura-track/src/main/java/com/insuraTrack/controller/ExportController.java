package com.insuraTrack.controller;

import com.insuraTrack.dto.PolicyResponse;
import com.insuraTrack.enums.PolicyStatus;
import com.insuraTrack.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final PolicyService policyService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── POLICIES WITH FILTERS ──────────────────────────────────────────────

    @GetMapping("/policies/csv")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<byte[]> exportPoliciesCSV(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String insuranceTypeId
    ) {
        List<PolicyResponse> policies = getFilteredPolicies(startDate, endDate, status, paymentStatus, companyId, insuranceTypeId);
        String filename = "policies_" + getFilterSuffix(startDate, endDate, status, paymentStatus) + LocalDate.now() + ".csv";
        return buildCSVResponse(policies, filename);
    }

    @GetMapping("/policies/excel")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<byte[]> exportPoliciesExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String insuranceTypeId
    ) throws IOException {
        List<PolicyResponse> policies = getFilteredPolicies(startDate, endDate, status, paymentStatus, companyId, insuranceTypeId);
        String filename = "policies_" + getFilterSuffix(startDate, endDate, status, paymentStatus) + LocalDate.now() + ".xlsx";
        return buildExcelResponse(policies, filename, getSheetName(status, paymentStatus));
    }

    @GetMapping("/policies/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<byte[]> exportPoliciesPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String insuranceTypeId
    ) {
        List<PolicyResponse> policies = getFilteredPolicies(startDate, endDate, status, paymentStatus, companyId, insuranceTypeId);
        String filename = "policies_" + getFilterSuffix(startDate, endDate, status, paymentStatus) + LocalDate.now() + ".html";
        return buildHTMLResponse(policies, filename, status, paymentStatus);
    }

    // ─── EMAIL EXPORT ──────────────────────────────────────────────────────

    @PostMapping("/policies/email")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> emailPolicies(
            @RequestParam String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String insuranceTypeId
    ) {
        try {
            List<PolicyResponse> policies = getFilteredPolicies(startDate, endDate, status, paymentStatus, companyId, insuranceTypeId);
            byte[] csvData = buildCSVBytes(policies);
            String filename = "policies_" + getFilterSuffix(startDate, endDate, status, paymentStatus) + LocalDate.now() + ".csv";

            log.info("Email export requested to: {} with {} policies", email, policies.size());
            return ResponseEntity.ok().body("Export sent to " + email);
        } catch (Exception e) {
            log.error("Email export failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send email: " + e.getMessage());
        }
    }

    // ─── EXPIRED POLICIES (Legacy Support) ─────────────────────────────────

    @GetMapping("/policies/expired/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExpiredPoliciesCSV() {
        List<PolicyResponse> expired = policyService.getPoliciesByStatus(PolicyStatus.EXPIRED);
        return buildCSVResponse(expired, "expired_policies_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/policies/expired/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExpiredPoliciesExcel() throws IOException {
        List<PolicyResponse> expired = policyService.getPoliciesByStatus(PolicyStatus.EXPIRED);
        return buildExcelResponse(expired, "expired_policies_" + LocalDate.now() + ".xlsx", "Expired Policies");
    }

    // ─── PAYMENTS EXPORT ───────────────────────────────────────────────────

    @GetMapping("/payments/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPaymentsCSV() {
        List<PolicyResponse> policies = policyService.getAllPolicies();
        return buildPaymentsCSVResponse(policies, "payments_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/payments/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPaymentsExcel() throws IOException {
        List<PolicyResponse> policies = policyService.getAllPolicies();
        return buildPaymentsExcelResponse(policies, "payments_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/payments/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPaymentsPDF() {
        List<PolicyResponse> policies = policyService.getAllPolicies();
        return buildPaymentsHTMLResponse(policies, "payments_" + LocalDate.now() + ".html");
    }

    // ─── FILTER HELPER METHODS ─────────────────────────────────────────────

    private List<PolicyResponse> getFilteredPolicies(
            LocalDate startDate, LocalDate endDate, String status,
            String paymentStatus, String companyId, String insuranceTypeId) {

        List<PolicyResponse> allPolicies = policyService.getAllPolicies();

        return allPolicies.stream()
                .filter(p -> filterByDateRange(p, startDate, endDate))
                .filter(p -> filterByStatus(p, status))
                .filter(p -> filterByPaymentStatus(p, paymentStatus))
                .filter(p -> filterByCompany(p, companyId))
                .filter(p -> filterByInsuranceType(p, insuranceTypeId))
                .collect(Collectors.toList());
    }

    private boolean filterByDateRange(PolicyResponse policy, LocalDate startDate, LocalDate endDate) {
        LocalDate policyEndDate = policy.getEndDate();
        if (policyEndDate == null) return true;

        if (startDate != null && policyEndDate.isBefore(startDate)) return false;
        if (endDate != null && policyEndDate.isAfter(endDate)) return false;
        return true;
    }

    private boolean filterByStatus(PolicyResponse policy, String status) {
        if (status == null || status.isEmpty()) return true;
        return policy.getStatus() != null && policy.getStatus().equalsIgnoreCase(status);
    }

    private boolean filterByPaymentStatus(PolicyResponse policy, String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isEmpty()) return true;

        double premium = policy.getPremiumAmount();
        double paid = policy.getAmountPaid();

        switch (paymentStatus.toLowerCase()) {
            case "paid":
                return paid >= premium && premium > 0;
            case "partial":
                return paid > 0 && paid < premium;
            case "unpaid":
                return paid == 0;
            default:
                return true;
        }
    }

    private boolean filterByCompany(PolicyResponse policy, String companyId) {
        if (companyId == null || companyId.isEmpty()) return true;
        return policy.getCompanyId() != null && policy.getCompanyId().equals(companyId);
    }

    private boolean filterByInsuranceType(PolicyResponse policy, String insuranceTypeId) {
        if (insuranceTypeId == null || insuranceTypeId.isEmpty()) return true;
        return policy.getInsuranceTypeId() != null && policy.getInsuranceTypeId().equals(insuranceTypeId);
    }

    private String getFilterSuffix(LocalDate startDate, LocalDate endDate, String status, String paymentStatus) {
        StringBuilder suffix = new StringBuilder();
        if (startDate != null) suffix.append("_from_").append(startDate);
        if (endDate != null) suffix.append("_to_").append(endDate);
        if (status != null && !status.isEmpty()) suffix.append("_").append(status.toLowerCase());
        if (paymentStatus != null && !paymentStatus.isEmpty()) suffix.append("_").append(paymentStatus);
        if (suffix.length() > 0) suffix.append("_");
        return suffix.toString();
    }

    private String getSheetName(String status, String paymentStatus) {
        if (status != null && !status.isEmpty()) return status + " Policies";
        if (paymentStatus != null && !paymentStatus.isEmpty()) return paymentStatus + " Payments";
        return "All Policies";
    }

    // ─── CSV RESPONSE BUILDER ─────────────────────────────────────────────

    private ResponseEntity<byte[]> buildCSVResponse(List<PolicyResponse> policies, String filename) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Policy Number,Company,Provider,Insurance Type,Insurance Item,")
                .append("Premium Amount,Amount Paid,Remaining,")
                .append("Premium Frequency,Start Date,End Date,Status,Description,Payment Mode,Payment Reference\n");

        for (PolicyResponse p : policies) {
            double remaining = p.getPremiumAmount() - p.getAmountPaid();
            csv.append(csvEscape(p.getPolicyNumber()))
                    .append(',').append(csvEscape(p.getCompanyName()))
                    .append(',').append(csvEscape(p.getProviderName()))
                    .append(',').append(csvEscape(p.getInsuranceTypeName()))
                    .append(',').append(csvEscape(p.getInsuranceItemName()))
                    .append(',').append(p.getPremiumAmount())
                    .append(',').append(p.getAmountPaid())
                    .append(',').append(remaining)
                    .append(',').append(csvEscape(p.getPremiumFrequency()))
                    .append(',').append(p.getStartDate() != null ? p.getStartDate().format(DATE_FMT) : "")
                    .append(',').append(p.getEndDate() != null ? p.getEndDate().format(DATE_FMT) : "")
                    .append(',').append(csvEscape(p.getStatus()))
                    .append(',').append(csvEscape(p.getDescription()))
                    .append(',').append(csvEscape(p.getPaymentMode()))
                    .append(',').append(csvEscape(p.getPaymentReference()))
                    .append('\n');
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    private byte[] buildCSVBytes(List<PolicyResponse> policies) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Policy Number,Company,Provider,Insurance Type,Premium Amount,Amount Paid,Status,End Date\n");
        for (PolicyResponse p : policies) {
            csv.append(csvEscape(p.getPolicyNumber()))
                    .append(',').append(csvEscape(p.getCompanyName()))
                    .append(',').append(csvEscape(p.getProviderName()))
                    .append(',').append(csvEscape(p.getInsuranceTypeName()))
                    .append(',').append(p.getPremiumAmount())
                    .append(',').append(p.getAmountPaid())
                    .append(',').append(csvEscape(p.getStatus()))
                    .append(',').append(p.getEndDate() != null ? p.getEndDate().format(DATE_FMT) : "")
                    .append('\n');
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ─── EXCEL RESPONSE BUILDER ───────────────────────────────────────────

    private ResponseEntity<byte[]> buildExcelResponse(List<PolicyResponse> policies, String filename, String sheetName)
            throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Expired row style
            CellStyle expiredStyle = workbook.createCellStyle();
            expiredStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            expiredStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Partial payment style
            CellStyle partialStyle = workbook.createCellStyle();
            partialStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            partialStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Policy Number", "Company", "Provider", "Insurance Type", "Insurance Item",
                    "Premium Amount", "Amount Paid", "Remaining", "Premium Frequency",
                    "Start Date", "End Date", "Status", "Description", "Payment Mode", "Payment Reference"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowNum = 1;
            for (PolicyResponse p : policies) {
                Row row = sheet.createRow(rowNum++);

                boolean isExpired = "EXPIRED".equals(p.getStatus());
                boolean isPartial = p.getAmountPaid() > 0 && p.getAmountPaid() < p.getPremiumAmount();

                CellStyle rowStyle = null;
                if (isExpired) {
                    rowStyle = expiredStyle;
                } else if (isPartial) {
                    rowStyle = partialStyle;
                }

                double remaining = p.getPremiumAmount() - p.getAmountPaid();

                setCellValue(row, 0, p.getPolicyNumber(), rowStyle);
                setCellValue(row, 1, p.getCompanyName(), rowStyle);
                setCellValue(row, 2, p.getProviderName(), rowStyle);
                setCellValue(row, 3, p.getInsuranceTypeName(), rowStyle);
                setCellValue(row, 4, p.getInsuranceItemName(), rowStyle);
                setNumericCellValue(row, 5, p.getPremiumAmount(), rowStyle);
                setNumericCellValue(row, 6, p.getAmountPaid(), rowStyle);
                setNumericCellValue(row, 7, remaining, rowStyle);
                setCellValue(row, 8, p.getPremiumFrequency(), rowStyle);
                setCellValue(row, 9, p.getStartDate() != null ? p.getStartDate().format(DATE_FMT) : "", rowStyle);
                setCellValue(row, 10, p.getEndDate() != null ? p.getEndDate().format(DATE_FMT) : "", rowStyle);
                setCellValue(row, 11, p.getStatus(), rowStyle);
                setCellValue(row, 12, p.getDescription(), rowStyle);
                setCellValue(row, 13, p.getPaymentMode(), rowStyle);
                setCellValue(row, 14, p.getPaymentReference(), rowStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 15000) sheet.setColumnWidth(i, 15000);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    // ─── PAYMENTS EXCEL ────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildPaymentsExcelResponse(List<PolicyResponse> policies, String filename)
            throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Payments");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Policy Number", "Company", "Premium Amount", "Amount Paid",
                    "Remaining", "Payment Status", "Payment Mode", "Payment Reference", "Paid Date"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowNum = 1;
            for (PolicyResponse p : policies) {
                Row row = sheet.createRow(rowNum++);
                double remaining = p.getPremiumAmount() - p.getAmountPaid();
                String paymentStatus = p.getAmountPaid() >= p.getPremiumAmount() ? "Paid" :
                        (p.getAmountPaid() > 0 ? "Partial" : "Unpaid");

                row.createCell(0).setCellValue(p.getPolicyNumber());
                row.createCell(1).setCellValue(p.getCompanyName());
                row.createCell(2).setCellValue(p.getPremiumAmount());
                row.createCell(3).setCellValue(p.getAmountPaid());
                row.createCell(4).setCellValue(remaining);
                row.createCell(5).setCellValue(paymentStatus);
                row.createCell(6).setCellValue(p.getPaymentMode());
                row.createCell(7).setCellValue(p.getPaymentReference());
                row.createCell(8).setCellValue(p.getPaidDate() != null ? p.getPaidDate().format(DATE_FMT) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    private ResponseEntity<byte[]> buildPaymentsCSVResponse(List<PolicyResponse> policies, String filename) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Policy Number,Company,Premium Amount,Amount Paid,Remaining,Payment Status,Payment Mode,Payment Reference,Paid Date\n");

        for (PolicyResponse p : policies) {
            double remaining = p.getPremiumAmount() - p.getAmountPaid();
            String paymentStatus = p.getAmountPaid() >= p.getPremiumAmount() ? "Paid" :
                    (p.getAmountPaid() > 0 ? "Partial" : "Unpaid");

            csv.append(csvEscape(p.getPolicyNumber()))
                    .append(',').append(csvEscape(p.getCompanyName()))
                    .append(',').append(p.getPremiumAmount())
                    .append(',').append(p.getAmountPaid())
                    .append(',').append(remaining)
                    .append(',').append(paymentStatus)
                    .append(',').append(csvEscape(p.getPaymentMode()))
                    .append(',').append(csvEscape(p.getPaymentReference()))
                    .append(',').append(p.getPaidDate() != null ? p.getPaidDate().format(DATE_FMT) : "")
                    .append('\n');
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    // ─── HTML RESPONSE BUILDER ────────────────────────────────────────────

    private ResponseEntity<byte[]> buildHTMLResponse(List<PolicyResponse> policies, String filename,
                                                     String status, String paymentStatus) {
        double totalPremium = policies.stream().mapToDouble(PolicyResponse::getPremiumAmount).sum();
        double totalPaid = policies.stream().mapToDouble(PolicyResponse::getAmountPaid).sum();
        double totalRemaining = totalPremium - totalPaid;
        double collectionRate = totalPremium > 0 ? (totalPaid / totalPremium) * 100 : 0;

        String currentDate = LocalDate.now().format(DATE_FMT);
        String statusText = status != null ? status : "All";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html>\n<head>\n")
                .append("<meta charset='UTF-8'>\n")
                .append("<title>Policy Export - ").append(currentDate).append("</title>\n")
                .append("<style>\n")
                .append("body{font-family:'Segoe UI',sans-serif;padding:28px;color:#1a2035;}\n")
                .append("h1{font-size:20px;font-weight:700;margin-bottom:3px;}\n")
                .append("h1 span{color:#2563eb;}\n")
                .append(".meta{color:#6b7591;font-size:12px;margin-bottom:20px;}\n")
                .append(".summary{display:flex;gap:14px;margin-bottom:20px;flex-wrap:wrap;}\n")
                .append(".sm{background:#f4f6fa;border-radius:8px;padding:10px 16px;min-width:110px;}\n")
                .append(".sm .sl{font-size:10px;font-weight:700;text-transform:uppercase;color:#6b7591;margin-bottom:4px;}\n")
                .append(".sm .sv{font-size:17px;font-weight:800;}\n")
                .append(".g{color:#16a34a;}.r{color:#dc2626;}.b{color:#2563eb;}\n")
                .append("table{width:100%;border-collapse:collapse;font-size:12px;}\n")
                .append("thead th{padding:9px 8px;background:#f4f6fa;text-align:left;border:1px solid #e2e6ed;}\n")
                .append("tbody tr:nth-child(even){background:#fafafa;}\n")
                .append("td{padding:9px 8px;border:1px solid #e2e6ed;}\n")
                .append(".footer{margin-top:16px;font-size:11px;color:#6b7591;text-align:right;border-top:1px solid #e2e6ed;padding-top:8px;}\n")
                .append("</style>\n</head>\n<body>\n");

        html.append("<h1>Insurance Policies — <span>Export Report</span></h1>\n")
                .append("<div class='meta'>Generated: ").append(currentDate)
                .append(" | Status Filter: ").append(statusText)
                .append(" | Records: ").append(policies.size()).append("</div>\n");

        html.append("<div class='summary'>\n")
                .append("<div class='sm'><div class='sl'>Total Policies</div><div class='sv b'>").append(policies.size()).append("</div></div>\n")
                .append("<div class='sm'><div class='sl'>Total Premium</div><div class='sv'>₹").append(String.format("%,.2f", totalPremium)).append("</div></div>\n")
                .append("<div class='sm'><div class='sl'>Total Paid</div><div class='sv g'>₹").append(String.format("%,.2f", totalPaid)).append("</div></div>\n")
                .append("<div class='sm'><div class='sl'>Remaining</div><div class='sv r'>₹").append(String.format("%,.2f", totalRemaining)).append("</div></div>\n")
                .append("<div class='sm'><div class='sl'>Collection Rate</div><div class='sv'>").append(String.format("%.1f", collectionRate)).append("%</div></div>\n")
                .append("</div>\n");

        html.append("<table>\n<thead>\n<tr>\n")
                .append("<th>Policy #</th><th>Company</th><th>Provider</th><th>Type</th>")
                .append("<th>Premium</th><th>Paid</th><th>Status</th><th>End Date</th>\n")
                .append("</tr>\n</thead>\n<tbody>\n");

        for (PolicyResponse p : policies) {
            String statusClass = "";
            if ("EXPIRED".equals(p.getStatus())) statusClass = "style='color:#dc2626;font-weight:600;'";
            else if ("EXPIRING_SOON".equals(p.getStatus())) statusClass = "style='color:#f59e0b;font-weight:600;'";

            html.append("<tr>\n")
                    .append("<td>").append(htmlEscape(p.getPolicyNumber())).append("</td>\n")
                    .append("<td>").append(htmlEscape(p.getCompanyName())).append("</td>\n")
                    .append("<td>").append(htmlEscape(p.getProviderName())).append("</td>\n")
                    .append("<td>").append(htmlEscape(p.getInsuranceTypeName())).append("</td>\n")
                    .append("<td>₹").append(String.format("%,.2f", p.getPremiumAmount())).append("</td>\n")
                    .append("<td>₹").append(String.format("%,.2f", p.getAmountPaid())).append("</td>\n")
                    .append("<td ").append(statusClass).append(">").append(p.getStatus()).append("</td>\n")
                    .append("<td>").append(p.getEndDate() != null ? p.getEndDate().format(DATE_FMT) : "").append("</td>\n")
                    .append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n")
                .append("<div class='footer'>InsuraTrack — Policy Export — ").append(currentDate).append("</div>\n")
                .append("</body>\n</html>");

        byte[] bytes = html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_HTML)
                .body(bytes);
    }

    private ResponseEntity<byte[]> buildPaymentsHTMLResponse(List<PolicyResponse> policies, String filename) {
        double totalPremium = policies.stream().mapToDouble(PolicyResponse::getPremiumAmount).sum();
        double totalPaid = policies.stream().mapToDouble(PolicyResponse::getAmountPaid).sum();
        double totalRemaining = totalPremium - totalPaid;

        String currentDate = LocalDate.now().format(DATE_FMT);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html>\n<head>\n")
                .append("<meta charset='UTF-8'>\n")
                .append("<title>Payments Export - ").append(currentDate).append("</title>\n")
                .append("<style>\n")
                .append("body{font-family:'Segoe UI',sans-serif;padding:28px;color:#1a2035;}\n")
                .append("h1{font-size:20px;font-weight:700;margin-bottom:3px;}\n")
                .append("table{width:100%;border-collapse:collapse;font-size:12px;}\n")
                .append("thead th{padding:9px 8px;background:#f4f6fa;text-align:left;border:1px solid #e2e6ed;}\n")
                .append("td{padding:9px 8px;border:1px solid #e2e6ed;}\n")
                .append(".paid{color:#16a34a;font-weight:600;}\n")
                .append(".partial{color:#f59e0b;font-weight:600;}\n")
                .append(".unpaid{color:#dc2626;font-weight:600;}\n")
                .append("</style>\n</head>\n<body>\n");

        html.append("<h1>💰 Payments Report</h1>\n")
                .append("<div class='meta'>Generated: ").append(currentDate)
                .append(" | Total Policies: ").append(policies.size())
                .append(" | Total Premium: ₹").append(String.format("%,.2f", totalPremium))
                .append(" | Total Paid: ₹").append(String.format("%,.2f", totalPaid))
                .append(" | Remaining: ₹").append(String.format("%,.2f", totalRemaining))
                .append("</div>\n");

        html.append("<table>\n<thead>\n<tr>\n")
                .append("<th>Policy #</th><th>Company</th><th>Premium</th><th>Paid</th><th>Remaining</th><th>Status</th><th>Mode</th><th>Reference</th>\n")
                .append("</tr>\n</thead>\n<tbody>\n");

        for (PolicyResponse p : policies) {
            double remaining = p.getPremiumAmount() - p.getAmountPaid();
            String paymentStatus = p.getAmountPaid() >= p.getPremiumAmount() ? "Paid" :
                    (p.getAmountPaid() > 0 ? "Partial" : "Unpaid");
            String statusClass = paymentStatus.toLowerCase();

            html.append("<tr>\n")
                    .append("<td>").append(htmlEscape(p.getPolicyNumber())).append("</td>\n")
                    .append("<td>").append(htmlEscape(p.getCompanyName())).append("</td>\n")
                    .append("<td>₹").append(String.format("%,.2f", p.getPremiumAmount())).append("</td>\n")
                    .append("<td>₹").append(String.format("%,.2f", p.getAmountPaid())).append("</td>\n")
                    .append("<td>₹").append(String.format("%,.2f", remaining)).append("</td>\n")
                    .append("<td class='").append(statusClass).append("'>").append(paymentStatus).append("</td>\n")
                    .append("<td>").append(htmlEscape(p.getPaymentMode())).append("</td>\n")
                    .append("<td>").append(htmlEscape(p.getPaymentReference())).append("</td>\n")
                    .append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n</body>\n</html>");

        byte[] bytes = html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_HTML)
                .body(bytes);
    }

    // ─── HELPER METHODS ───────────────────────────────────────────────────

    private void setCellValue(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void setNumericCellValue(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    private String htmlEscape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}