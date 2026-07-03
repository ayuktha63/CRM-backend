package com.orque.crm.report.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.report.dto.*;
import com.orque.crm.report.entity.DashboardPreference;
import com.orque.crm.report.repository.DashboardPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final JdbcTemplate jdbcTemplate;
    private final DashboardPreferenceRepository dashboardPreferenceRepository;
    /** Resolves the owner to scope queries to. Non-admins always see only their own data. */
    private String resolveScope(String scopedUsername) {
        if (!UserContextHelper.isAdmin()) {
            return UserContextHelper.currentUsername();
        }
        return scopedUsername;
    }

    /** Returns the org_id clause fragment for use in WHERE clauses — always non-null in CRM. */
    private String orgClause(String table) {
        String orgId = UserContextHelper.currentOrganizationId();
        if (orgId == null) return "";
        String col = (table != null ? table + "." : "") + "organization_id";
        return " AND " + col + " = '" + orgId.replace("'", "''") + "'";
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(String scopedUsername) {
        String owner = resolveScope(scopedUsername);
        String org = orgClause(null);
        return DashboardSummaryResponse.builder()
                .totalContacts(countP("SELECT COUNT(*) FROM contacts WHERE 1=1" + org + (owner != null ? " AND owner = ?" : ""), owner))
                .totalLeads(countP("SELECT COUNT(*) FROM leads WHERE 1=1" + org + (owner != null ? " AND assigned_owner = ?" : ""), owner))
                .hotLeads(countP("SELECT COUNT(*) FROM leads WHERE status = 'QUALIFIED'" + org + (owner != null ? " AND assigned_owner = ?" : ""), owner))
                .tasksDueToday(countP("SELECT COUNT(*) FROM crm_tasks WHERE DATE(due_date) = CURRENT_DATE" + org + (owner != null ? " AND assigned_to = ?" : ""), owner))
                .revenueGenerated(sumP("SELECT COALESCE(SUM(estimated_value), 0) FROM leads WHERE pipeline_stage = 'WON'" + org + (owner != null ? " AND assigned_owner = ?" : ""), owner))
                .pipelineValue(sumP("SELECT COALESCE(SUM(estimated_value), 0) FROM leads WHERE pipeline_stage NOT IN ('WON', 'LOST')" + org + (owner != null ? " AND assigned_owner = ?" : ""), owner))
                .totalCampaigns(count("SELECT COUNT(*) FROM campaigns WHERE 1=1" + org))
                .emailsSent(count("SELECT COALESCE(SUM(m.sent_count), 0) FROM campaign_metrics m JOIN campaigns c ON c.id = m.campaign_id WHERE 1=1" + orgClause("c")))
                .emailsOpened(count("SELECT COALESCE(SUM(m.opened_count), 0) FROM campaign_metrics m JOIN campaigns c ON c.id = m.campaign_id WHERE 1=1" + orgClause("c")))
                .emailsReplied(count("SELECT COALESCE(SUM(m.replied_count), 0) FROM campaign_metrics m JOIN campaigns c ON c.id = m.campaign_id WHERE 1=1" + orgClause("c")))
                .build();
    }

    @Override
    public List<MonthlySalesTrendResponse> getMonthlySalesTrend(String scopedUsername) {
        String owner = resolveScope(scopedUsername);
        String org = orgClause(null);
        String ownerClause = owner != null ? " AND assigned_owner = '" + owner.replace("'", "''") + "'" : "";

        String sql = "SELECT TO_CHAR(created_at, 'YYYY-MM') AS month, COUNT(*) AS leads_created, COALESCE(SUM(estimated_value), 0) AS estimated_value FROM leads WHERE 1=1" + org + ownerClause + " GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY month";

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
        String owner = resolveScope(null);
        String org = orgClause(null);
        String ownerClause = owner != null ? " AND assigned_owner = '" + owner.replace("'", "''") + "'" : "";

        String sql = "SELECT lead_source AS source, COUNT(*) AS count FROM leads WHERE 1=1" + org + ownerClause + " GROUP BY lead_source";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                LeadSourceDistributionResponse.builder()
                        .source(rs.getString("source"))
                        .count(rs.getLong("count"))
                        .build()
        );
    }

    @Override
    public List<ConversionFunnelResponse> getConversionFunnel() {
        String owner = resolveScope(null);
        String org = orgClause(null);
        String ownerClause = owner != null ? " AND assigned_owner = '" + owner.replace("'", "''") + "'" : "";

        String sql = "SELECT pipeline_stage AS stage, COUNT(*) AS count FROM leads WHERE 1=1" + org + ownerClause + " GROUP BY pipeline_stage";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                ConversionFunnelResponse.builder()
                        .stage(rs.getString("stage"))
                        .count(rs.getLong("count"))
                        .build()
        );
    }

    @Override
    public List<RecentActivityResponse> getRecentActivities() {
        String owner = resolveScope(null);
        String ownerClause = owner != null ? " AND performed_by = '" + owner.replace("'", "''") + "'" : "";

        String sql = "SELECT 'AUDIT' AS module, action AS activity_type, description AS description, created_at AS activity_at FROM audit_logs WHERE 1=1" + ownerClause + " ORDER BY created_at DESC LIMIT 10";

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

    /** count with optional single String bind param; pass null for unscoped. */
    private Long countP(String sql, String param) {
        if (param == null) return jdbcTemplate.queryForObject(sql, Long.class);
        return jdbcTemplate.queryForObject(sql, Long.class, param);
    }

    /** sum with optional single String bind param; pass null for unscoped. */
    private BigDecimal sumP(String sql, String param) {
        if (param == null) return jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, param);
    }


    @Override
    public ReportSummaryResponse getPipelinePerformanceReport() {
        String org = orgClause(null);
        var data = jdbcTemplate.queryForList(
                "SELECT pipeline_stage, COUNT(*) AS lead_count, COALESCE(SUM(estimated_value), 0) AS total_value FROM leads WHERE 1=1" + org + " GROUP BY pipeline_stage");
        return buildReport("Pipeline Performance Report", data);
    }


    @Override
    public ReportSummaryResponse getMonthlySalesReport() {
        return buildReport("Monthly Sales Report", getMonthlySalesTrend(null));
    }



    @Override
    public ReportSummaryResponse getContactGrowthReport() {
        String org = orgClause(null);
        var data = jdbcTemplate.queryForList(
                "SELECT TO_CHAR(created_at, 'YYYY-MM') AS month, COUNT(*) AS contacts_created FROM contacts WHERE 1=1" + org + " GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY month");
        return buildReport("Contact Growth Report", data);
    }

    @Override
    public ReportSummaryResponse getTaskPerformanceReport() {
        String org = orgClause(null);
        var data = jdbcTemplate.queryForList(
                "SELECT status, priority, COUNT(*) AS task_count FROM crm_tasks WHERE 1=1" + org + " GROUP BY status, priority");
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