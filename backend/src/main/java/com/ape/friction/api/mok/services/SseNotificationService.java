// S3_NOTIF Aktywne powiadamianie użytkownika o wybranym zdarzeniu dotyczącym jego konta


package com.ape.friction.api.mok.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


@RequiredArgsConstructor
@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID accountId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);


        emitters.computeIfAbsent(accountId, k -> new CopyOnWriteArrayList<>()).add(emitter);


        emitter.onCompletion(() -> {
            log.info("[SSE] COMPLETION accountId={}, emitter={}", accountId, System.identityHashCode(emitter));
            removeEmitter(accountId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("[SSE] TIMEOUT accountId={}, emitter={}", accountId, System.identityHashCode(emitter));
            removeEmitter(accountId, emitter);
        });
        emitter.onError((e) -> {
            log.info("[SSE] ERROR accountId={}, emitter={}, error={}", accountId, System.identityHashCode(emitter), e.getMessage());
            removeEmitter(accountId, emitter);
        });

        return emitter;
    }

    @PreAuthorize("hasAuthority('GET_MY_NOTIFICATIONS')")
    public void notifyAccount(UUID accountId, String eventAction) {
        List<SseEmitter> userEmitters = emitters.get(accountId);

        log.info("[SSE] NOTIFY accountId={}, action={}, emittersFound={}, this={}, allKeys={}",
                accountId, eventAction,
                userEmitters == null ? "NULL" : userEmitters.size(),
                System.identityHashCode(this),
                emitters.keySet());

        if (userEmitters == null || userEmitters.isEmpty()) {
            log.warn("[SSE] NO_EMITTERS accountId={}, action={}", accountId, eventAction);
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                log.info("[SSE] SENDING to emitter={}", System.identityHashCode(emitter));
                emitter.send(SseEmitter.event().name("AccountEvent").data(eventAction));
                log.info("[SSE] SENT_OK emitter={}", System.identityHashCode(emitter));
            } catch (Exception e) {
                log.error("[SSE] SEND_FAILED emitter={}, error={}", System.identityHashCode(emitter), e.getMessage());
                emitter.completeWithError(e);
            }
        }
    }

    private void removeEmitter(UUID accountId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(accountId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(accountId);
            }
        }
    }


    @Scheduled(fixedRate = 20000)
    public void sendPingToAll() {
        emitters.forEach((accountId, userEmitters) -> {
            for (SseEmitter emitter : userEmitters) {
                try {
                    log.info("We have connection of notifications with accountId={}" , accountId);
                    emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                } catch (Exception e) {
                    log.info("This user will not have notifications now accountId={}" , accountId);
                    emitter.complete();
                    removeEmitter(accountId, emitter);
                }
            }
        });
    }


    @PostConstruct
    public void init() {
        log.info("[SSE] SseNotificationService INSTANCE CREATED: {}", System.identityHashCode(this));
    }
}
