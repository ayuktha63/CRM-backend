package com.orque.crm.notification.repository;

import com.orque.crm.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String username);
    long countByRecipientUsernameAndIsReadFalse(String username);
}
