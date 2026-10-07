package com.ape.friction.api.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ape.friction.api.common.Language;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.entities.users.Administrator;
import com.ape.friction.api.entities.users.LoginAudit;
import com.ape.friction.api.events.AdminAuthenticated;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.LoginAuditRepository;


import java.time.Instant;


@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class SessionLoggingInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SessionLoggingInterceptor.class);

    private final ApplicationEventPublisher eventPublisher;

    private final LoginAuditRepository loginAuditRepository;

    private final AccountRepository accountRepository;


    @AfterReturning(value = "Pointcuts.sessionLoggedAnnotatedMethods()")
    public void registerSession(JoinPoint joinPoint) {
        try {
            String login = "unknown";

            String ip = getClientIp();

            for (Object arg : joinPoint.getArgs()) {
                Account account = (Account) arg;
                login = account.getLogin();

                LoginAudit loginAudit = loginAuditRepository.findByAccount(account)
                        .orElse(new LoginAudit(account));
                loginAudit.setLastLoginAt(Instant.now());
                loginAudit.setLastSuccessfulAuthIp(ip);
                loginAuditRepository.saveAndFlush(loginAudit);

                String langString = extractLanguageFromRequest();
                Language language = convertToLanguage(langString);
                account.setLanguage(language);
                accountRepository.saveAndFlush(account);

                if (account.getAccessLevels().stream()
                        .anyMatch(accessLevel -> accessLevel instanceof Administrator)) {
                    eventPublisher.publishEvent(

                            new AdminAuthenticated(account.getId(), ip)
                    );

                }
                break;
            }
            log.info("Session started for user: '{}' from IP: {}", login, ip);
        } catch (Exception e) {
            log.warn("SessionLoggingInterceptor failed",
                    e
            );
        }
    }

    private String extractLanguageFromRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "en";
        String header = attrs.getRequest().getHeader("Accept-Language");
        return (header != null && !header.isEmpty()) ? header.split(",")[0].split("-")[0] : "en";
    }

    private Language convertToLanguage(String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            return Language.EN;
        }
        try {
            return Language.valueOf(langCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Language.EN;
        }
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest request = attrs.getRequest();
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
