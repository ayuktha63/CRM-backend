package com.orque.crm.report.service;

import com.orque.crm.report.dto.*;

import java.util.List;

public interface ReportService {

    /** Returns stats scoped to {@code scopedUsername}. Pass null for admin combined view. */
    DashboardSummaryResponse getDashboardSummary(String scopedUsername);

    List<MonthlySalesTrendResponse> getMonthlySalesTrend(String scopedUsername);

    List<LeadSourceDistributionResponse> getLeadSourceDistribution();

    List<ConversionFunnelResponse> getConversionFunnel();

    List<RecentActivityResponse> getRecentActivities();

    ReportSummaryResponse getLeadConversionReport(ReportFilterRequest filter);

    ReportSummaryResponse getPipelinePerformanceReport();

    ReportSummaryResponse getRevenueReport(ReportFilterRequest filter);

    ReportSummaryResponse getMonthlySalesReport();

    ReportSummaryResponse getCampaignAnalyticsReport(ReportFilterRequest filter);

    ReportSummaryResponse getContactGrowthReport();

    ReportSummaryResponse getTaskPerformanceReport();

    ReportSummaryResponse getActivityReport(ReportFilterRequest filter);

    List<DashboardPreferenceResponse> getDashboardPreferences(Long userId);

    void saveDashboardPreferences(
            Long userId,
            List<DashboardPreferenceRequest> requests
    );
}