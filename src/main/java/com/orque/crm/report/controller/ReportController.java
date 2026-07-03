package com.orque.crm.report.controller;

import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.enums.RoleType;
import com.orque.crm.report.dto.*;
import com.orque.crm.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.orque.crm.report.export.CsvExportService;
import com.orque.crm.report.export.ExcelExportService;
import com.orque.crm.report.export.PdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CsvExportService csvExportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final UserRepository userRepository;

    /**
     * List all sales users — admin-only endpoint for the dashboard user-picker dropdown.
     * Returns [{username, displayName}] sorted by username.
     */
    @GetMapping("/admin/users")
    public ResponseEntity<List<Map<String, String>>> listSalesUsers() {
        if (!UserContextHelper.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        String orgId = UserContextHelper.scopedOrgId();
        List<Map<String, String>> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null)
                .filter(u -> u.getRole().getName() != RoleType.ADMIN)
                .filter(u -> orgId == null || orgId.equals(u.getOrganizationId()))
                .sorted(java.util.Comparator.comparing(u -> u.getUsername()))
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "displayName", (u.getFirstName() != null ? u.getFirstName() : "")
                                + (u.getLastName() != null ? " " + u.getLastName() : "").trim()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam(required = false) String username) {
        return ResponseEntity.ok(reportService.getDashboardSummary(username));
    }

    @GetMapping("/monthly-sales")
    public ResponseEntity<List<MonthlySalesTrendResponse>> getMonthlySalesTrend(
            @RequestParam(required = false) String username) {
        return ResponseEntity.ok(reportService.getMonthlySalesTrend(username));
    }

    @GetMapping("/lead-source-distribution")
    public ResponseEntity<List<LeadSourceDistributionResponse>> getLeadSourceDistribution() {
        return ResponseEntity.ok(reportService.getLeadSourceDistribution());
    }

    @GetMapping("/conversion-funnel")
    public ResponseEntity<List<ConversionFunnelResponse>> getConversionFunnel() {
        return ResponseEntity.ok(reportService.getConversionFunnel());
    }

    @GetMapping("/recent-activities")
    public ResponseEntity<List<RecentActivityResponse>> getRecentActivities() {
        return ResponseEntity.ok(reportService.getRecentActivities());
    }
    @GetMapping("/lead-conversion")
    public ResponseEntity<ReportSummaryResponse> getLeadConversionReport(
            @ModelAttribute ReportFilterRequest filter
    ) {
        return ResponseEntity.ok(
                reportService.getLeadConversionReport(filter)
        );
    }

    @GetMapping("/pipeline-performance")
    public ResponseEntity<ReportSummaryResponse> getPipelinePerformanceReport() {
        return ResponseEntity.ok(
                reportService.getPipelinePerformanceReport()
        );
    }

    @GetMapping("/revenue")
    public ResponseEntity<ReportSummaryResponse> getRevenueReport(
            @ModelAttribute ReportFilterRequest filter
    ) {
        return ResponseEntity.ok(
                reportService.getRevenueReport(filter)
        );
    }

    @GetMapping("/monthly-sales-report")
    public ResponseEntity<ReportSummaryResponse> getMonthlySalesReport() {
        return ResponseEntity.ok(
                reportService.getMonthlySalesReport()
        );
    }

    @GetMapping("/campaign-analytics")
    public ResponseEntity<ReportSummaryResponse> getCampaignAnalyticsReport(
            @ModelAttribute ReportFilterRequest filter
    ) {
        return ResponseEntity.ok(
                reportService.getCampaignAnalyticsReport(filter)
        );
    }
    @GetMapping("/contact-growth")
    public ResponseEntity<ReportSummaryResponse> getContactGrowthReport() {
        return ResponseEntity.ok(
                reportService.getContactGrowthReport()
        );
    }

    @GetMapping("/task-performance")
    public ResponseEntity<ReportSummaryResponse> getTaskPerformanceReport() {
        return ResponseEntity.ok(
                reportService.getTaskPerformanceReport()
        );
    }

    @GetMapping("/activity")
    public ResponseEntity<ReportSummaryResponse> getActivityReport(
            @ModelAttribute ReportFilterRequest filter
    ) {
        return ResponseEntity.ok(
                reportService.getActivityReport(filter)
        );
    }
    @GetMapping("/dashboard/preferences/{userId}")
    public ResponseEntity<List<DashboardPreferenceResponse>>
    getDashboardPreferences(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                reportService.getDashboardPreferences(userId)
        );
    }

    @PutMapping("/dashboard/preferences/{userId}")
    public ResponseEntity<String>
    saveDashboardPreferences(
            @PathVariable Long userId,
            @RequestBody List<DashboardPreferenceRequest> requests
    ) {

        reportService.saveDashboardPreferences(
                userId,
                requests
        );


        return ResponseEntity.ok(
                "Dashboard preferences saved successfully"
        );
    }
    @GetMapping("/export/csv")
    public ResponseEntity<String> exportCsv() {

        var report = reportService.getLeadConversionReport(
                new ReportFilterRequest()
        );

        @SuppressWarnings("unchecked")
        String csv = csvExportService.exportToCsv(
                (List<java.util.Map<String, Object>>) report.getData()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"crm-report.csv\""                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel()
            throws Exception {

        var report = reportService.getLeadConversionReport(
                new ReportFilterRequest()
        );

        @SuppressWarnings("unchecked")
        byte[] excel = excelExportService.exportToExcel(
                (List<java.util.Map<String, Object>>) report.getData()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"crm-report.xlsx\""                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf()
            throws Exception {

        var report = reportService.getLeadConversionReport(
                new ReportFilterRequest()
        );

        @SuppressWarnings("unchecked")
        byte[] pdf = pdfExportService.exportToPdf(
                (List<java.util.Map<String, Object>>) report.getData()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"crm-report.pdf\""                )
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(pdf);
    }
}