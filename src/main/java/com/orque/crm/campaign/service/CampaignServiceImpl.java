package com.orque.crm.campaign.service;

import com.orque.crm.campaign.dto.*;
import com.orque.crm.campaign.entity.*;
import com.orque.crm.campaign.repository.*;
import com.orque.crm.enums.CampaignRecipientStatus;
import com.orque.crm.enums.CampaignStatus;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.orque.crm.audit.service.AuditLogService;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.enums.AuditAction;
import com.orque.crm.enums.AuditModule;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final CampaignEmailHistoryRepository campaignEmailHistoryRepository;
    private final CampaignMetricsRepository campaignMetricsRepository;
    private final CampaignAsyncService campaignAsyncService;
    private final AuditLogService auditLogService;

    @Override
    public CampaignResponse createCampaign(CreateCampaignRequest request) {

        Campaign campaign = Campaign.builder()
                .campaignName(request.getCampaignName())
                .subjectLine(request.getSubjectLine())
                .emailBody(request.getEmailBody())
                .mailboxId(request.getMailboxId())
                .organizationId(UserContextHelper.currentOrganizationId())
                .status(CampaignStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();

        Campaign saved = campaignRepository.save(campaign);
        log.info("Campaign saved");

        CampaignMetrics metrics = CampaignMetrics.builder()
                .campaignId(saved.getId())
                .totalRecipients(0)
                .sentCount(0)
                .failedCount(0)
                .deliveredCount(0)
                .openedCount(0)
                .repliedCount(0)
                .build();

        campaignMetricsRepository.save(metrics);
        auditLogService.createAudit(
                AuditAction.CAMPAIGN_CREATED,
                AuditModule.CAMPAIGN,
                "Campaign",
                saved.getId(),
                null,
                saved.getCampaignName(),
                "Campaign created: " + saved.getCampaignName(),
                "SYSTEM",
                null
        );

        return mapCampaignToResponse(saved);
    }

    @Override
    public List<CampaignResponse> getAllCampaigns() {
        String orgId = UserContextHelper.scopedOrgId();
        List<Campaign> campaigns;
        if (orgId == null) {
            campaigns = campaignRepository.findAll();
        } else {
            campaigns = campaignRepository.findByOrganizationId(orgId);
        }
        return campaigns.stream()
                .map(this::mapCampaignToResponse)
                .toList();
    }

    @Override
    public CampaignResponse getCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        return mapCampaignToResponse(campaign);
    }

    /**
     * Campaigns have no owner field — they're tenant-shared like Inventory, so this is
     * an org-only check (not UserContextHelper.assertAccess, which also enforces an
     * owner match and would incorrectly block every non-admin user since owner is
     * always null here).
     */
    @Override
    public CampaignResponse updateCampaign(Long campaignId, CreateCampaignRequest request) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        String myOrg = UserContextHelper.currentOrganizationId();
        if (myOrg != null && campaign.getOrganizationId() != null && !myOrg.equals(campaign.getOrganizationId())) {
            throw new RuntimeException("Access denied.");
        }

        campaign.setCampaignName(request.getCampaignName());
        campaign.setSubjectLine(request.getSubjectLine());
        campaign.setEmailBody(request.getEmailBody());
        campaign.setMailboxId(request.getMailboxId());
        if (request.getStatus() != null) {
            campaign.setStatus(request.getStatus());
        }

        Campaign saved = campaignRepository.save(campaign);
        return mapCampaignToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        String myOrg = UserContextHelper.currentOrganizationId();
        if (myOrg != null && campaign.getOrganizationId() != null && !myOrg.equals(campaign.getOrganizationId())) {
            throw new RuntimeException("Access denied.");
        }

        List<CampaignRecipient> recipients = campaignRecipientRepository.findByCampaignId(campaignId);
        if (!recipients.isEmpty()) {
            campaignRecipientRepository.deleteAll(recipients);
        }

        List<CampaignEmailHistory> histories = campaignEmailHistoryRepository.findByCampaignId(campaignId);
        if (!histories.isEmpty()) {
            campaignEmailHistoryRepository.deleteAll(histories);
        }

        campaignMetricsRepository.findByCampaignId(campaignId)
                .ifPresent(campaignMetricsRepository::delete);

        campaignRepository.delete(campaign);

        auditLogService.createAudit(
                AuditAction.CAMPAIGN_DELETED,
                AuditModule.CAMPAIGN,
                "Campaign",
                campaignId,
                campaign.getCampaignName(),
                null,
                "Campaign deleted: " + campaign.getCampaignName(),
                "SYSTEM",
                null
        );
    }

    @Override
    public CampaignRecipientResponse addRecipient(
            Long campaignId,
            AddRecipientsRequest request
    ) {

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() ->
                        new RuntimeException("Campaign not found"));

        CampaignRecipient recipient = CampaignRecipient.builder()
                .campaignId(campaign.getId())
                .contactId(request.getContactId())
                .leadId(request.getLeadId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .company(request.getCompany())
                .email(request.getEmail())
                .status(CampaignRecipientStatus.PENDING)
                .build();

        CampaignRecipient saved =
                campaignRecipientRepository.save(recipient);

        CampaignMetrics metrics =
                campaignMetricsRepository.findByCampaignId(campaignId)
                        .orElseThrow(() ->
                                new RuntimeException("Campaign metrics not found"));

        metrics.setTotalRecipients(metrics.getTotalRecipients() + 1);

        campaignMetricsRepository.save(metrics);

        return mapRecipientToResponse(saved);
    }

    @Override
    public List<CampaignRecipientResponse> getRecipients(Long campaignId) {

        return campaignRecipientRepository.findByCampaignId(campaignId)
                .stream()
                .map(this::mapRecipientToResponse)
                .toList();
    }

    @Override
    public void launchCampaign(Long campaignId) {

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() ->
                        new RuntimeException("Campaign not found"));

        List<CampaignRecipient> recipients =
                campaignRecipientRepository.findByCampaignId(campaignId);

        if (recipients.isEmpty()) {
            throw new RuntimeException("No recipients found for this campaign");
        }

        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setLaunchedAt(LocalDateTime.now());

        campaignRepository.save(campaign);
        auditLogService.createAudit(
                AuditAction.CAMPAIGN_LAUNCHED,
                AuditModule.CAMPAIGN,
                "Campaign",
                campaign.getId(),
                CampaignStatus.DRAFT.name(),
                CampaignStatus.RUNNING.name(),
                "Campaign launched: " + campaign.getCampaignName(),
                "SYSTEM",
                null
        );

        campaignAsyncService.processCampaign(campaign);
    }
    @Override
    public List<CampaignHistoryResponse> getCampaignHistory(Long campaignId) {

        return campaignEmailHistoryRepository.findByCampaignId(campaignId)
                .stream()
                .map(this::mapHistoryToResponse)
                .toList();
    }

    @Override
    public CampaignMetricsResponse getCampaignMetrics(Long campaignId) {

        CampaignMetrics metrics =
                campaignMetricsRepository.findByCampaignId(campaignId)
                        .orElseThrow(() ->
                                new RuntimeException("Campaign metrics not found"));

        return CampaignMetricsResponse.builder()
                .totalRecipients(metrics.getTotalRecipients())
                .sentCount(metrics.getSentCount())
                .failedCount(metrics.getFailedCount())
                .deliveredCount(metrics.getDeliveredCount())
                .openedCount(metrics.getOpenedCount())
                .repliedCount(metrics.getRepliedCount())
                .build();
    }

    private CampaignResponse mapCampaignToResponse(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .campaignName(campaign.getCampaignName())
                .subjectLine(campaign.getSubjectLine())
                .emailBody(campaign.getEmailBody())
                .mailboxId(campaign.getMailboxId())
                .status(campaign.getStatus())
                .createdAt(campaign.getCreatedAt())
                .launchedAt(campaign.getLaunchedAt())
                .completedAt(campaign.getCompletedAt())
                .build();
    }
    private CampaignRecipientResponse mapRecipientToResponse(
            CampaignRecipient recipient
    ) {
        return CampaignRecipientResponse.builder()
                .id(recipient.getId())
                .campaignId(recipient.getCampaignId())
                .firstName(recipient.getFirstName())
                .lastName(recipient.getLastName())
                .company(recipient.getCompany())
                .email(recipient.getEmail())
                .status(recipient.getStatus())
                .sentAt(recipient.getSentAt())
                .openedAt(recipient.getOpenedAt())
                .repliedAt(recipient.getRepliedAt())
                .build();
    }
    private CampaignHistoryResponse mapHistoryToResponse(
            CampaignEmailHistory history
    ) {
        return CampaignHistoryResponse.builder()
                .id(history.getId())
                .toEmail(history.getToEmail())
                .subject(history.getSubject())
                .status(history.getStatus())
                .sentAt(history.getSentAt())
                .build();
    }

}