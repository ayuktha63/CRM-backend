package com.orque.crm.notification.controller;

import com.orque.crm.notification.entity.Notification;
import com.orque.crm.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@CrossOrigin
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        return ResponseEntity.ok(service.getMyNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(service.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        service.markAllAsRead();
        return ResponseEntity.ok().build();
    }
}
