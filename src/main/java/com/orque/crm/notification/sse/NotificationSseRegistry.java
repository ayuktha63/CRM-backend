package com.orque.crm.notification.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks open SSE connections per username so notification pushes replace the old
 * interval(30000) client-side poll — the browser holds one long-lived connection
 * instead of asking "anything new?" every 30 seconds regardless of whether
 * anything actually changed.
 */
@Component
public class NotificationSseRegistry {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes per connection

    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter register(String username) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> list = emittersByUser.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> list.remove(emitter));
        emitter.onError(e -> list.remove(emitter));

        return emitter;
    }

    /** Push a "notification changed" event to every open connection for this user. */
    public void notifyUser(String username) {
        List<SseEmitter> list = emittersByUser.get(username);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data("update"));
            } catch (IOException e) {
                emitter.completeWithError(e);
                list.remove(emitter);
            }
        }
    }
}
