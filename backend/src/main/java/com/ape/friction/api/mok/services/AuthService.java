package com.ape.friction.api.mok.services;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.common.FrontendUrlBuilder;
import com.ape.friction.api.common.Language;
import com.ape.friction.api.dto.response.login.Login2FARequiredResponseDTO;
import com.ape.friction.api.dto.response.login.LoginRequiresPasswordChange;
import com.ape.friction.api.dto.response.login.LoginResponseDTO;
import com.ape.friction.api.dto.response.login.LoginSuccessResponseDTO;
import com.ape.friction.api.entities.token.AuthenticationToken;
import com.ape.friction.api.entities.token.EmailChangeToken;
import com.ape.friction.api.entities.token.PasswordResetToken;
import com.ape.friction.api.entities.token.VerificationToken;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.entities.users.Client;
import com.ape.friction.api.events.AccountStatusChangedEvent;
import com.ape.friction.api.exceptions.*;
import com.ape.friction.api.exceptions.jwt.JwtInvalidToken;
import com.ape.friction.api.exceptions.tokenAndLoginCodes.*;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
public class AuthService {

    @Value("${security.account.lock-duration-hours:24}")
    private long lockDurationHours;

    @Value("${security.account.tries.to-lock:3}")
    private long triesToLock;

    private final AccountRepository accountRepository;

    private final PasswordEncoder passwordEncoder;

    private final VerificationTokenRepository verificationTokenRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final MailService mailService;

    private final FrontendUrlBuilder frontendUrlBuilder;

    private final Authentication2FARepository authentication2FARepository;

    private final SecureRandom secureRandom;

    private final JwtService jwtService;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final EmailChangeTokenRepository emailChangeTokenRepository;

    private final PasswordUpdateService passwordUpdateService;

    private final AccountModerationGuard accountModerationGuard;

    private static final List<AccountLoginGuard> LOGIN_GUARDS = List.of(
            new AccountLoginGuard(Account::isVerified, NotVerifiedAccountException::new),
            new AccountLoginGuard(Account::isActive, DisabledAccountException::new)
    );

    private record AccountLoginGuard(
            java.util.function.Predicate<Account> passes,
            java.util.function.Supplier<? extends RuntimeException> onFail) {
    }


    // MOK.1
    @Value("${account.verification.token.validity.hours}")
    private long verificationTokenValidityHours;
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    public Account register(Account account, Locale locale) {
        String normalizedEmail = account.getEmail().trim().toLowerCase();
        String normalizedLogin = account.getLogin().trim().toLowerCase();
        String normalizedName = account.getName().trim();
        String normalizedSurname = account.getSurname().trim();

        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setVerified(false);
        account.setActive(true);
        account.setLogin(normalizedLogin);
        account.setEmail(normalizedEmail);
        account.setName(normalizedName);
        account.setSurname(normalizedSurname);

        Client client = Client.builder()
                .account(account)
                .active(true)
                .build();

        account.getAccessLevels().add(client);

        String localeStr = locale.getLanguage().toUpperCase();

        boolean isValidLanguage = Arrays.stream(Language.values())
                .anyMatch(lang -> lang.name().equals(localeStr));

        if (!isValidLanguage) {
            account.setLanguage(Language.EN);
        } else {
            account.setLanguage(Language.valueOf(localeStr));
        }

        Account saved = accountRepository.saveAndFlush(account);

        // token valid for 24h
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(tokenValue);
        verificationToken.setAccount(saved);
        verificationToken.setExpiresDate(Instant.now().plus(verificationTokenValidityHours, ChronoUnit.HOURS));
        verificationTokenRepository.save(verificationToken);

        String link = frontendUrlBuilder.withToken("/auth/verify", tokenValue);
        mailService.sendVerificationMail(saved.getEmail(), link, Locale.forLanguageTag(account.getLanguage().toString()));

        return saved;
    }

    // MOK.1
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    public void verifyAccount(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .filter(t -> t.getExpiresDate().isAfter(Instant.now()))
                .orElseThrow(() -> {
                    verificationTokenRepository.findByToken(token)
                            .ifPresent(expired -> verificationTokenRepository.delete(expired));
                    return new TokenNotFoundException();
                });

        Account account = verificationToken.getAccount();
        account.setVerified(true);
        accountRepository.saveAndFlush(account);

        verificationTokenRepository.delete(verificationToken);
        eventPublisher.publishEvent(
                new AccountStatusChangedEvent(account.getId(), "verified")
        );
    }
    private void tryUnlockAccount(Account account) {
        if (!account.isActive() && account.getLockUntil() != null && Instant.now().isAfter(account.getLockUntil())) {
            account.setActive(true);
            account.setFailedAttempts(0);
            account.setLockUntil(null);
            eventPublisher.publishEvent(new AccountStatusChangedEvent(account.getId(), "activated"));
            accountRepository.saveAndFlush(account);
        }
    }

    private void validatePasswordAndBlock(Account account, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, account.getPassword())) {

            int attempts = account.getFailedAttempts() + 1; //logika blokowania po 3 nieudanych próbach
            account.setFailedAttempts(attempts);
            if (attempts >= triesToLock) {
                account.setLockUntil(Instant.now().plus(lockDurationHours, ChronoUnit.HOURS));
                account.setActive(false);
                eventPublisher.publishEvent(
                        new AccountStatusChangedEvent(account.getId(), "deactivated")
                );
            }
            accountRepository.saveAndFlush(account);
            throw new BadPasswordException();
        }

        account.setFailedAttempts(0); //reszet bo sie udało
        accountRepository.saveAndFlush(account);
    }

    private LoginResponseDTO checkIfAccountRequiresPasswordChange(Account account) {
        if (account.isRequiredNewPassword()) {
            return LoginRequiresPasswordChange.builder()
                    .id(account.getId())
                    .version(account.getVersion())
                    .build();

        }

//        String clientIp = IpUtils.getClientIp();
//        loginAttemptLoggingService.logSuccess(account, clientIp);

        List<String> tokens =
                jwtService.authenticate(account);

        return LoginSuccessResponseDTO.builder()
                .accessToken(tokens.get(0))
                .refreshToken(tokens.get(1))
                .build();

    }

    private LoginResponseDTO handleTwoFactorOrSuccess(Account account) {

        if (!account.isTwoFaEnabled()) {
            return checkIfAccountRequiresPasswordChange(account); // nie trzeba 2fa no to sprawdzamy czy nie musi zmienić hasełka
        }

        // sprzątanie starych tokenów
        authentication2FARepository.deleteAllByAccountAndExpiresAtBefore(account, Instant.now());

        authentication2FARepository.findByAccount(account).ifPresent(existing -> {
            throw new AlreadyRequestedLogin2FAException();
        });

        String secretCode = String.format("%08d", secureRandom.nextInt(100_000_000));
        UUID authSessionId = UUID.randomUUID();

        AuthenticationToken authenticationToken = new AuthenticationToken();
        authenticationToken.setAuthSessionId(authSessionId);
        authenticationToken.setToken(passwordEncoder.encode(secretCode));
        authenticationToken.setAccount(account);
        authenticationToken.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        authentication2FARepository.saveAndFlush(authenticationToken);

        mailService.send2FA(account.getEmail(), secretCode,Locale.forLanguageTag(account.getLanguage().toString()));
        return Login2FARequiredResponseDTO.builder()
                .requires2FA(true)
                .authSessionId(authSessionId)
                .build();
    }

    //MOK.2
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager",noRollbackFor = BadPasswordException.class)
    public LoginResponseDTO login(String login, String password) {
        Account account = accountRepository.findByLogin(login.trim().toLowerCase())
                .orElseThrow(BadLoginCredentialsException::new);

        tryUnlockAccount(account); // Próbujemy odblokować konto, które zostało zablokowane przez nieudane próby logowań (rozwiązanie zamiast schedule)

        validatePasswordAndBlock(account, password); // Próba logowania

        LOGIN_GUARDS.stream()
                .filter(guard -> !guard.passes().test(account))
                .findFirst()
                .ifPresent(guard -> {
                    throw guard.onFail().get();
                });


        if (accountModerationGuard.isRestricted(account.getId())) {
            throw new DisabledAccountException();
        }

        return handleTwoFactorOrSuccess(account); // No i teraz jak login i hasło sie zgadza przechodzimy do metody, która jeszcze sprawdza czy nie jest 2fa właczone
    }


    //MOK.2
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    public List<String> refresh(String refreshToken) {


        Claims claims = jwtService.parseClaims(refreshToken)
                .orElseThrow(JwtInvalidToken::new);


        String type = claims.get("type", String.class);
        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtInvalidToken();
        }

        String userId = claims.getSubject();
        Account account = accountRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (accountModerationGuard.isRestricted(account.getId())) {
            throw new DisabledAccountException();
        }

        LOGIN_GUARDS.stream()
                .filter(guard -> !guard.passes().test(account))
                .findFirst()
                .ifPresent(guard -> { throw guard.onFail().get(); });

        return jwtService.refresh(account);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    public void resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> optionalToken =
                passwordResetTokenRepository.findByToken(token);

        optionalToken.ifPresent(t -> System.out.println("TOKEN FOUND: " + t));

        PasswordResetToken resetToken = optionalToken
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> {
                    optionalToken.ifPresent(t -> {
                        System.out.println("TOKEN EXPIRED: " + t);
                        passwordResetTokenRepository.delete(t);
                        throw new TokenExpiredException();
                    });

                    System.out.println("TOKEN NOT FOUND OR EXPIRED: " + token);

                    return new TokenNotFoundException();
                });

        Account account = resetToken.getAccount();
        //uniqpwd

        passwordUpdateService.changePasswordWithHistory(account, newPassword);

        passwordResetTokenRepository.delete(resetToken);
    }

    // S2_KODYJEDN — weryfikacja jednorazowego kodu wysłanego mailem.
    // Wygasłe tokeny zostają w bazie i są sprzątane przy kolejnym login()
    // przez deleteAllByAccountAndExpiresAtBefore.
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager", noRollbackFor = Token2FAExpiredException.class)
    public LoginResponseDTO check2fA(UUID authSessionId, String token) {
        AuthenticationToken trueToken = authentication2FARepository.findByAuthSessionId(authSessionId)
                .orElseThrow(Token2FANotFoundException::new);

        if (Instant.now().isAfter(trueToken.getExpiresAt())) {
            authentication2FARepository.delete(trueToken);

            throw new Token2FAExpiredException();
        }
        if (!passwordEncoder.matches(token, trueToken.getToken())) {

            throw new Token2FAInvalidCodeException();
        }
        Account account = trueToken.getAccount();
        authentication2FARepository.delete(trueToken);

        return checkIfAccountRequiresPasswordChange(account); // no tutaj też nie ucieknie przed zmianą hasła

    }

    //MOK.15 + MOK.16
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager", noRollbackFor = TokenExpiredException.class)
    public void finishingChangingEmail(String token) {
        Optional<EmailChangeToken> optionalToken = emailChangeTokenRepository.findByToken(token);

        optionalToken.ifPresent(t -> {
            System.out.println("TOKEN change emailFOUND: " + t);
        });


        EmailChangeToken changeToken = optionalToken
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> {
                    optionalToken.ifPresent(t -> {
                        System.out.println("TOKEN EXPIRED: " + t);
                        emailChangeTokenRepository.delete(t);
                        emailChangeTokenRepository.flush();
                        throw new TokenExpiredException();
                    });


                    return new TokenNotFoundException();
                });

        Account account = changeToken.getAccount();
        String oldEmail = changeToken.getOldEmail();
        String newEmail = changeToken.getNewEmail();

        account.setEmail(newEmail);
        accountRepository.saveAndFlush(account);

        emailChangeTokenRepository.delete(changeToken);
        emailChangeTokenRepository.flush();

        String tokenToOldEmail = UUID.randomUUID().toString();

        EmailChangeToken resetEmailToken = new EmailChangeToken();
        resetEmailToken.setToken(tokenToOldEmail);
        // Mimo że newEmail teraz jest głównym, nadal zostanie jako nowy żeby się nie mylić
        resetEmailToken.setNewEmail(newEmail);
        resetEmailToken.setOldEmail(oldEmail);
        resetEmailToken.setAccount(account);
        resetEmailToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        emailChangeTokenRepository.saveAndFlush(resetEmailToken);

        String link = frontendUrlBuilder.withToken("/auth/change-email-to-old", tokenToOldEmail);
        mailService.sendChangeToOldEmailOfAccount(oldEmail, newEmail, link, Locale.forLanguageTag(account.getLanguage().toString()));

    }

    //MOK.15 + MOK.16
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager", noRollbackFor = TokenExpiredException.class)
    public void finishingChangingEmailToOldOne(String token) {
        Optional<EmailChangeToken> optionalToken = emailChangeTokenRepository.findByToken(token);

        optionalToken.ifPresent(t -> {
        });


        EmailChangeToken changeToken = optionalToken
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> {
                    optionalToken.ifPresent(t -> {
                        emailChangeTokenRepository.delete(t);
                        emailChangeTokenRepository.flush();

                        throw new TokenExpiredException();
                    });


                    return new TokenNotFoundException();
                });

        Account account = changeToken.getAccount();
        String oldEmail = changeToken.getOldEmail();

        account.setEmail(oldEmail);
        accountRepository.saveAndFlush(account);

        emailChangeTokenRepository.delete(changeToken);
        emailChangeTokenRepository.flush();

    }
//
//    // EXPMSG - extend currently authenticated user's session and return a fresh access+refresh pair.
//    // Unlike /auth/refresh this is driven by the access token already in SecurityContext
//    // (the user clicks "Extend session" while still authenticated), so it does not require
//    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
//    public List<String> extendSession(String login) {
//        Account account = accountRepository.findByLogin(login.trim().toLowerCase())
//                .orElseThrow(AccountNotFoundException::new);
//
//        LOGIN_GUARDS.stream()
//                .filter(guard -> !guard.passes().test(account))
//                .findFirst()
//                .ifPresent(guard -> { throw guard.onFail().get(); });
//
//        return Arrays.asList(
//                jwtService.generateAccessToken(account),
//                jwtService.generateRefreshToken(account)
//        );
//    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    public void setForcedPasswordChange(String newPassword, UUID id, long version) {
        Account target = accountRepository.findById(id)
                .filter(Account::isRequiredNewPassword)
                .orElseThrow(AccountNotFoundException::new);

        if (!target.getVersion().equals(version)) {
            throw new ConcurrentUpdateException();
        }
        target.setRequiredNewPassword(false);
        passwordUpdateService.changePasswordWithHistory(target, newPassword);
    }

}
