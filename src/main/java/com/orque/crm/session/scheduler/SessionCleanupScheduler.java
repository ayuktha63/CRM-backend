package com.orque.crm.session.scheduler;

import com.orque.crm.session.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {

    private final UserSessionRepository sessionRepository;

    // Every Sunday 02:00 server time. ACTIVE sessions are untouched by the query
    // regardless of age; only TERMINATED/EXPIRED rows older than a week are purged.
    @Scheduled(cron = "0 0 2 * * SUN")
    public void purgeStaleClosedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusWeeks(1);
        int deleted = sessionRepository.deleteStaleClosedSessions(cutoff);
        log.info("Session cleanup: purged {} TERMINATED/EXPIRED session row(s) older than {}", deleted, cutoff);
    }
}
