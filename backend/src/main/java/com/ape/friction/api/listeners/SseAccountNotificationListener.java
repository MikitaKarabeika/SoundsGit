// S3_NOTIF Aktywne powiadamianie użytkownika o wybranym zdarzeniu dotyczącym jego konta

package com.ape.friction.api.listeners;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.ape.friction.api.events.AccountStatusChangedEvent;
import com.ape.friction.api.mok.services.SseNotificationService;

@Component
@RequiredArgsConstructor
public class SseAccountNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(SseAccountNotificationListener.class);

    private final SseNotificationService sseNotificationService;

    //@EventListener
    //@Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountStatusChanged(AccountStatusChangedEvent event) {
        try {
            sseNotificationService.notifyAccount(event.id(), event.cause());
            log.info("Notified this user:", event.id(), " about ", event.cause());
        } catch (Exception e) {
            log.info("Exception while listening notifications", e);
        }
    }
}
