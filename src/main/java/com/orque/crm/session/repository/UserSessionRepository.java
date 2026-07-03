package com.orque.crm.session.repository;

import com.orque.crm.session.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    List<UserSession> findByStatus(String status);

    List<UserSession> findByUserId(Long userId);

    Optional<UserSession> findByJwtId(String jwtId);

    List<UserSession> findByStatusAndLastActivityBefore(String status, LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.status = 'EXPIRED' WHERE s.status = 'ACTIVE' AND s.lastActivity < :cutoff")
    int expireOldSessions(LocalDateTime cutoff);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.status = 'ACTIVE'")
    long countActiveSessions();

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.loginTime >= :since")
    long countLoginsToday(LocalDateTime since);

    long countByUsernameAndStatus(String username, String status);

    List<UserSession> findByUsernameIn(java.util.Collection<String> usernames);
}
