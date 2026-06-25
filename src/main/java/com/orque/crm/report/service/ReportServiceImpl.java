package com.orque.crm.report.service;

import com.orque.crm.report.dto.*;
import com.orque.crm.report.entity.DashboardPreference;
import com.orque.crm.report.repository.DashboardPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final JdbcTemplate jdbcTemplate;
    private final DashboardPreferenceRepository dashboardPreferenceRepository;
    @Override
    public DashboardSummaryResponse getDashboardSummary() {

        return DashboardSummaryResponse.builder()
                .totalContacts(count("SELECT COUNT(*) FROM contacts"))
                .totalLeads(count("SELECT COUNT(*) FROM leads"))
                .hotLeads(count("SELECT COUNT(*) FROM leads WHERE status = 'QUALIFIED'"))
                .tasksDueToday(count("""
                        SELECT COUNT(*) FROM crm_tasks
                        WHERE DATE(due_date) = CURRENT_DATE
                        """))
                .revenueGenerated(sum("""
                        SELECT COALESCE(SUM(estimated_value), 0)
                        FROM leads
                        WHERE pipeline_stage = 'WON'
                        """))
                .pipelineValue(sum("""
                        SELECT COALESCE(SUM(estimated_value), 0)
                        FROM leads
                        WHERE pipeline_stage NOT IN ('WON', 'LOST')
                        """))
                .totalCampaigns(count("SELECT COUNT(*) FROM campaigns"))
                .emailsSent(count("SELECT COALESCE(SUM(sent_count), 0) FROM campaign_metrics"))
                .emailsOpened(count("SELECT COALESCE(SUM(opened_count), 0) FROM campaign_metrics"))
                .emailsReplied(count("SELECT COALESCE(SUM(replied_count), 0) FROM campaign_metrics"))
                .build();
    }

    @Override
    public List<MonthlySalesTrendResponse> getMonthlySalesTrend() {

        String sql = """
                SELECT TO_CHAR(created_at, 'YYYY-MM') AS month,
                       COUNT(*) AS leads_created,
                       COALESCE(SUM(estimated_value), 0) AS estimated_value
                FROM leads
                GROUP BY TO_CHAR(created_at, 'YYYY-MM')
                ORDER BY month
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                MonthlySalesTrendResponse.builder()
                        .month(rs.getString("month"))
                        .leadsCreated(rs.getLong("leads_created"))
                        .estimatedValue(rs.getBigDecimal("estimated_value"))
                        .build()
        );
    }

    @Override
    public List<LeadSourceDistributionResponse> getLeadSourceDistribution() {

        String sql = """
                SELECT lead_source AS source,
                       COUNT(*) AS count
                FROM leads
                GROUP BY lead_source
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                LeadSourceDistributionResponse.builder()
                        .source(rs.getString("source"))
                        .count(rs.getLong("count"))
                        .build()
        );
    }

    @Override
    public List<ConversionFunnelResponse> getConversionFunnel() {

        String sql = """
                SELECT pipeline_stage AS stage,
                       COUNT(*) AS count
                FROM leads
                GROUP BY pipeline_stage
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                ConversionFunnelResponse.builder()
                        .stage(rs.getString("stage"))
                        .count(rs.getLong("count"))
                        .build()
        );
    }

    @Override
    public List<RecentActivityResponse> getRecentActivities() {

        String sql = """
                SELECT 'AUDIT' AS module,
                       action AS activity_type,
                       description AS description,
                       created_at AS activity_at
                FROM audit_logs
                ORDER BY created_at DESC
                LIMIT 10
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                RecentActivityResponse.builder()
                        .module(rs.getString("module"))
                        .activityType(rs.getString("activity_type"))
                        .description(rs.getString("description"))
                        .activityAt(rs.getTimestamp("activity_at").toLocalDateTime())
                        .build()
        );
    }

    private Long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private BigDecimal sum(String sql) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }


    @Override
    public ReportSummaryResponse getPipelinePerformanceReport() {
        var data = jdbcTemplate.queryForList("""
            SELECT pipeline_stage, COUNT(*) AS lead_count,
                   COALESCE(SUM(estimated_value), 0) AS total_value
            FROM leads
            GROUP BY pipeline_stage
            """);

        return buildReport("Pipeline Performance Report", data);
    }


    @Override
    public ReportSummaryResponse getMonthlySalesReport() {
        return buildReport("Monthly Sales Report", getMonthlySalesTrend());
    }



    @Override
    public ReportSummaryResponse getContactGrowthReport() {
        var data = jdbcTemplate.queryForList("""
            SELECT TO_CHAR(created_at, 'YYYY-MM') AS month,
                   COUNT(*) AS contacts_created
            FROM contacts
            GROUP BY TO_CHAR(created_at, 'YYYY-MM')
            ORDER BY month
            """);

        return buildReport("Contact Growth Report", data);
    }

    @Override
    public ReportSummaryResponse getTaskPerformanceReport() {
        var data = jdbcTemplate.queryForList("""
            SELECT status, priority, COUNT(*) AS task_count
            FROM crm_tasks
            GROUP BY status, priority
            """);

        return buildReport("Task Performance Report", data);
    }



    private ReportSummaryResponse buildReport(String reportName, Object data) {

        Long totalRecords = 0L;

        if (data instanceof List<?> list) {
            totalRecords = (long) list.size();
        }

        return ReportSummaryResponse.builder()
                .reportName(reportName)
                .totalRecords(totalRecords)
                .generatedAt(LocalDateTime.now().toString())
                .data(data)
                .build();
    }
    @Override
    public List<DashboardPreferenceResponse> getDashboardPreferences(Long userId) {

        return dashboardPreferenceRepository
                .findByUserIdOrderByDisplayOrder(userId)
                .stream()
                .map(preference ->
                        DashboardPreferenceResponse.builder()
                                .widgetType(preference.getWidgetType())
                                .displayOrder(preference.getDisplayOrder())
                                .visible(preference.getVisible())
                                .build()
                )
                .toList();
    }

    @Override
    public void saveDashboardPreferences(
            Long userId,
            List<DashboardPreferenceRequest> requests
    ) {

        dashboardPreferenceRepository.deleteByUserId(userId);

        List<DashboardPreference> preferences =
                requests.stream()
                        .map(request ->
                                DashboardPreference.builder()
                                        .userId(userId)
                                        .widgetType(request.getWidgetType())
                                        .displayOrder(request.getDisplayOrder())
                                        .visible(request.getVisible())
                                        .build()
                        )
                        .toList();

        dashboardPreferenceRepository.saveAll(preferences);
    }
    @Override
    public ReportSummaryResponse getLeadConversionReport(ReportFilterRequest filter) {

        StringBuilder sql = new StringBuilder("""
            SELECT lead_source, status, pipeline_stage, COUNT(*) AS count
            FROM leads
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (filter.getLeadSource() != null) {
            sql.append(" AND lead_source = ?");
            params.add(filter.getLeadSource());
        }

        if (filter.getLeadStatus() != null) {
            sql.append(" AND status = ?");
            params.add(filter.getLeadStatus());
        }

        if (filter.getStartDate() != null) {
            sql.append(" AND DATE(created_at) >= ?");
            params.add(filter.getStartDate());
        }

        if (filter.getEndDate() != null) {
            sql.append(" AND DATE(created_at) <= ?");
            params.add(filter.getEndDate());
        }

        sql.append(" GROUP BY lead_source, status, pipeline_stage");

        var data = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return buildReport("Lead Conversion Report", data);
    }

    @Override
    public ReportSummaryResponse getRevenueReport(ReportFilterRequest filter) {

        StringBuilder sql = new StringBuilder("""
            SELECT pipeline_stage,
                   COALESCE(SUM(estimated_value), 0) AS revenue
            FROM leads
            WHERE pipeline_stage = 'WON'
            """);

        List<Object> params = new ArrayList<>();

        if (filter.getLeadSource() != null) {
            sql.append(" AND lead_source = ?");
            params.add(filter.getLeadSource());
        }

        if (filter.getLeadStatus() != null) {
            sql.append(" AND status = ?");
            params.add(filter.getLeadStatus());
        }

        if (filter.getStartDate() != null) {
            sql.append(" AND DATE(created_at) >= ?");
            params.add(filter.getStartDate());
        }

        if (filter.getEndDate() != null) {
            sql.append(" AND DATE(created_at) <= ?");
            params.add(filter.getEndDate());
        }

        sql.append(" GROUP BY pipeline_stage");

        var data = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return buildReport("Revenue Report", data);
    }

    @Override
    public ReportSummaryResponse getCampaignAnalyticsReport(ReportFilterRequest filter) {

        StringBuilder sql = new StringBuilder("""
            SELECT c.id, c.campaign_name, c.status,
                   m.total_recipients, m.sent_count, m.delivered_count,
                   m.opened_count, m.replied_count, m.failed_count
            FROM campaigns c
            LEFT JOIN campaign_metrics m ON c.id = m.campaign_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (filter.getCampaignStatus() != null) {
            sql.append(" AND c.status = ?");
            params.add(filter.getCampaignStatus());
        }

        if (filter.getStartDate() != null) {
            sql.append(" AND DATE(c.created_at) >= ?");
            params.add(filter.getStartDate());
        }

        if (filter.getEndDate() != null) {
            sql.append(" AND DATE(c.created_at) <= ?");
            params.add(filter.getEndDate());
        }

        sql.append(" ORDER BY c.id DESC");

        var data = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return buildReport("Campaign Analytics Report", data);
    }

    @Override
    public ReportSummaryResponse getActivityReport(ReportFilterRequest filter) {

        StringBuilder sql = new StringBuilder("""
            SELECT action, description, created_at
            FROM audit_logs
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (filter.getStartDate() != null) {
            sql.append(" AND DATE(created_at) >= ?");
            params.add(filter.getStartDate());
        }

        if (filter.getEndDate() != null) {
            sql.append(" AND DATE(created_at) <= ?");
            params.add(filter.getEndDate());
        }

        sql.append(" ORDER BY created_at DESC LIMIT 50");

        var data = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        return buildReport("Activity Report", data);
    }
}