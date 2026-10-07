package com.ape.friction.api.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.events.AdminAuthenticated;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.services.MailService;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AdminAuthenticatedListener {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthenticatedListener.class);
    private final MailService mailService;
    private final AccountRepository accountRepository;

    @EventListener
    @Async
    public void on(AdminAuthenticated event) {

        try{
            Account account = accountRepository.findById(event.id())
                    .orElseThrow();

            Locale locale = Locale.forLanguageTag(account.getLanguage().toString());

            mailService.sendNotificationToAdmin(
                    account.getEmail(),
                    event.ip(),
                    account.getLogin(),
                    Instant.now().atZone(ZoneId.of(account.getTimezone())),
                    locale
            );

        } catch (Exception e){
            log.info("Exception while listening admin authenticated", e);
        }

    }

}
