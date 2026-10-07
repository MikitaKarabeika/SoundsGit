package com.ape.friction.api.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.events.AccountStatusChangedEvent;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.services.MailService;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AccountNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AccountNotificationListener.class);
    private final AccountRepository accountRepository;
    private final MailService mailService;

    @EventListener
    @Async
    public void on(AccountStatusChangedEvent event) {

        try {
            Account account = accountRepository.findById(event.id())
                    .orElseThrow();

            Locale locale = Locale.forLanguageTag(account.getLanguage().toString());

            mailService.sendNotification(
                    account.getEmail(),
                    event.cause(),
                    locale
            );

        } catch (Exception e){
            log.info("Exception while listening account notifications", e);
        }

    }
}
