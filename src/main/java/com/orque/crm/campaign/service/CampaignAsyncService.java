package com.orque.crm.campaign.service;

import com.orque.crm.campaign.entity.*;
import com.orque.crm.campaign.repository.*;
import com.orque.crm.enums.CampaignRecipientStatus;
import com.orque.crm.enums.CampaignStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignAsyncService {

    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final CampaignEmailHistoryRepository campaignEmailHistoryRepository;
    private final CampaignMetricsRepository campaignMetricsRepository;

    @Async
    public void processCampaign(Campaign campaign) {

        List<CampaignRecipient> recipients =
                campaignRecipientRepository.findByCampaignId(campaign.getId());

        int sentCount = 0;
        int failedCount = 0;

        List<CampaignEmailHistory> historiesToSave = new java.util.ArrayList<>();

        for (CampaignRecipient recipient : recipients) {

            try {

                String subject = personalize(
                        campaign.getSubjectLine(),
                        recipient
                );

                String body = personalize(
                        campaign.getEmailBody(),
                        recipient
                );

                recipient.setStatus(CampaignRecipientStatus.SENT);
                recipient.setSentAt(LocalDateTime.now());

                CampaignEmailHistory history =
                        CampaignEmailHistory.builder()
                                .campaignId(campaign.getId())
                                .recipientId(recipient.getId())
                                .toEmail(recipient.getEmail())
                                .subject(subject)
                                .body(body)
                                .status(CampaignRecipientStatus.SENT)
                                .createdAt(LocalDateTime.now())
                                .sentAt(LocalDateTime.now())
                                .build();

                historiesToSave.add(history);

                sentCount++;

            } catch (Exception e) {

                recipient.setStatus(CampaignRecipientStatus.FAILED);
                recipient.setErrorMessage(e.getMessage());

                failedCount++;
            }
        }

        // Persist all recipient status changes and email history rows in two batched
        // calls instead of one save() per recipient inside the loop above.
        campaignRecipientRepository.saveAll(recipients);
        if (!historiesToSave.isEmpty()) {
            campaignEmailHistoryRepository.saveAll(historiesToSave);
        }

        CampaignMetrics metrics =
                campaignMetricsRepository.findByCampaignId(campaign.getId())
                        .orElseThrow();

        metrics.setSentCount(sentCount);
        metrics.setFailedCount(failedCount);
        metrics.setDeliveredCount(sentCount);

        campaignMetricsRepository.save(metrics);

        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setCompletedAt(LocalDateTime.now());

        campaignRepository.save(campaign);
    }

    private String personalize(
            String text,
            CampaignRecipient recipient
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace("{{firstName}}", recipient.getFirstName() == null ? "" : recipient.getFirstName())
                .replace("{{lastName}}", recipient.getLastName() == null ? "" : recipient.getLastName())
                .replace("{{company}}", recipient.getCompany() == null ? "" : recipient.getCompany())
                .replace("{{email}}", recipient.getEmail() == null ? "" : recipient.getEmail());
    }
}