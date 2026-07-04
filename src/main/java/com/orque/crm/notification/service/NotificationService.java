package com.orque.crm.notification.service;

import com.orque.crm.notification.entity.Notification;
import com.orque.crm.notification.repository.NotificationRepository;
import com.orque.crm.notification.sse.NotificationSseRegistry;
import com.orque.crm.common.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationSseRegistry sseRegistry;

    public List<Notification> getMyNotifications() {
        String username = UserContextHelper.currentUsername();
        return repository.findByRecipientUsernameOrderByCreatedAtDesc(username);
    }

    public long getUnreadCount() {
        String username = UserContextHelper.currentUsername();
        return repository.countByRecipientUsernameAndIsReadFalse(username);
    }

    @Transactional
    public Notification addNotification(String recipient, String title, String message, String link) {
        Notification notification = Notification.builder()
                .recipientUsername(recipient)
                .title(title)
                .message(message)
                .link(link)
                .isRead(false)
                .build();
        Notification saved = repository.save(notification);
        sseRegistry.notifyUser(recipient);
        return saved;
    }

    @Transactional
    public void markAsRead(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found"));
        notification.setIsRead(true);
        repository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        String username = UserContextHelper.currentUsername();
        List<Notification> unread = repository.findByRecipientUsernameOrderByCreatedAtDesc(username);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        repository.saveAll(unread);
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter openStream() {
        String username = UserContextHelper.currentUsername();
        return sseRegistry.register(username);
    }
}
