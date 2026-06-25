package com.orque.crm.campaign.entity;

import com.orque.crm.enums.CampaignRecipientStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_email_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignEmailHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campaignId;

    private Long recipientId;

    private String toEmail;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String gmailMessageId;

    private String gmailThreadId;

    @Enumerated(EnumType.STRING)
    private CampaignRecipientStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;
}