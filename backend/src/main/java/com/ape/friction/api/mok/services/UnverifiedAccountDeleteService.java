package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.common.FrontendUrlBuilder;
import com.ape.friction.api.entities.token.VerificationToken;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccessLevelRepository;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.VerificationTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(
        propagation = Propagation.REQUIRES_NEW,
        transactionManager = "mokTransactionManager",
        readOnly = true
)
public class UnverifiedAccountDeleteService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final AccountRepository accountRepository;
    private final AccessLevelRepository accessLevelRepository;
    private final MailService mailService;
    private final FrontendUrlBuilder frontendUrlBuilder;

    @Value("${account.verification.token.validity.hours}")
    private long verificationTokenValidityHours;


    // uruchamia się co godzinę
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            transactionManager = "mokTransactionManager",
            readOnly = false
    )
    public void deleteExpiredUnverifiedAccounts() {
        Instant now = Instant.now();

        sendVerificationReminders(now);
        deleteExpiredAccounts(now);

        verificationTokenRepository.flush();
    }

    private void sendVerificationReminders(Instant now) {
        List<VerificationToken> tokens =
                verificationTokenRepository.findAllByReminderSentFalseAndExpiresDateAfter(now);

        for (VerificationToken token : tokens) {
            Account account = token.getAccount();

            if (account == null) {
                token.setReminderSent(true);
                verificationTokenRepository.save(token);
                continue;
            }

            if (account.isVerified()) {
                token.setReminderSent(true);
                verificationTokenRepository.save(token);
                continue;
            }

            Instant reminderTime = token.getCreatedAt()// half of token's time
                    .plus(verificationTokenValidityHours / 2, ChronoUnit.HOURS);

            if (now.isBefore(reminderTime)) {
                continue;
            }

            String link = frontendUrlBuilder.withToken("/auth/verify", token.getToken());

            mailService.sendReminderVerificationMail(
                    account.getEmail(),
                    link,
                    Locale.forLanguageTag(account.getLanguage().toString())
            );

            token.setReminderSent(true);
            verificationTokenRepository.save(token);
        }
    }

    private void deleteExpiredAccounts(Instant now) {
        List<VerificationToken> expiredTokens =
                verificationTokenRepository.findAllByExpiresDateBefore(now);

        for (VerificationToken token : expiredTokens) {
            Account account = token.getAccount();

            if (account == null) {
                verificationTokenRepository.delete(token);
                continue;
            }

            if (account.isVerified()) {
                verificationTokenRepository.delete(token);
                continue;
            }

            String email = account.getEmail();
            Locale locale = Locale.forLanguageTag(account.getLanguage().toString());

            verificationTokenRepository.delete(token);
            account.getAccessLevels().forEach(accessLevelRepository::delete);
            accountRepository.delete(account);

            mailService.sendAccountDeletedAfterNoVerificationMail(email, locale);
        }
    }
}
