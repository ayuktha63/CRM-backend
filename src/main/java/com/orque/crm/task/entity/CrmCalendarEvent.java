package com.orque.crm.task.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_calendar_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    private String recurrenceRule; // NONE, DAILY, WEEKLY, MONTHLY

    private String colorCategory; // Hex code color

    private String meetingRoom;

    @Column(columnDefinition = "TEXT")
    private String invitees; // Comma-separated emails

    private Integer reminderMinutes; // Minutes before start to notify user

    private String createdBy;
    private String organizationId;
    private LocalDateTime createdAt;

    // Time Zone and Sync
    private String timeZone;
    private String syncSource;
    private String syncId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.recurrenceRule == null) {
            this.recurrenceRule = "NONE";
        }
        if (this.colorCategory == null) {
            this.colorCategory = "#3B82F6"; // default blue
        }
    }
}
