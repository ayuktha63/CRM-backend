package com.orque.crm.license.dto;

import com.orque.crm.enums.LicenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseStatusResponse {
    private Long id;
    private String organizationId;
    private String organizationName;
    private String licenseName;
    private String orgCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private int gracePeriodDays;
    private int maximumUsers;
    private int concurrentUsers;
    private LicenseStatus status;
    private int daysRemaining;    // days to expiry (negative = expired)
    private int graceRemaining;   // grace days left (0 if not in grace)
    private boolean inGracePeriod;
    private int activeSessionCount;
    private int currentUserCount;
    private LocalDateTime activatedAt;
    private java.util.List<String> features;
}
