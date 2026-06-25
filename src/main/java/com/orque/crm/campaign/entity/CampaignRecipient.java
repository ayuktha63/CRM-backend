package com.orque.crm.campaign.entity;

import com.orque.crm.enums.CampaignRecipientStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_recipients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campaignId;

    private Long contactId;

    private Long leadId;

    private String firstName;

    private String lastName;

    private String company;

    private String email;

    @Enumerated(EnumType.STRING)
    private CampaignRecipientStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime openedAt;

    private LocalDateTime repliedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}