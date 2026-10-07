package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.common.FrontendUrlBuilder;
import com.ape.friction.api.entities.token.PasswordResetToken;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.PasswordResetTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Component
@Service
@RequiredArgsConstructor
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW,  readOnly = false, transactionManager = "mokTransactionManager")
public class PasswordResetService {

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final FrontendUrlBuilder frontendUrlBuilder;

    @Transactional(propagation = Propagation.REQUIRES_NEW,  readOnly = false, transactionManager = "mokTransactionManager")
    public String sendResetLink(String email) {
        return accountRepository.findByEmailIgnoreCase(email)
                .filter(Account::isActive)
                .filter(Account::isVerified)
                .map(account -> {
                    tokenRepository.findByAccount(account).ifPresent(tokenRepository::delete);

                    String token = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setToken(token);
                    resetToken.setAccount(account);
                    resetToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
                    tokenRepository.saveAndFlush(resetToken);

                    String link = frontendUrlBuilder.withToken("/auth/reset-password", token);
                    mailService.sendResetPasswordMail(account.getEmail(), link, Locale.forLanguageTag(account.getLanguage().toString()));

                    return account.getEmail();
                })
                .orElse(email);
    }

}
