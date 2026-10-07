package com.ape.friction.api.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ape.friction.api.dto.request.LoginCodeDTO;
import com.ape.friction.api.dto.request.LoginRequestDTO;
import com.ape.friction.api.dto.request.TwoFARequestDTO;
import com.ape.friction.api.mok.services.LoginAttemptLoggingService;

@Aspect
@Order
@Component
@RequiredArgsConstructor
public class FailedLoginInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FailedLoginInterceptor.class);
    private final LoginAttemptLoggingService loginAttemptLoggingService;

    @AfterThrowing(value = "@annotation(LogFailedLogin)", throwing = "exception")
    public void registerFailedLogin(JoinPoint joinPoint, Exception exception) {
        try {

            String login =
                    extractLoginFromArgs(joinPoint.getArgs());

            if (login != null) {

                loginAttemptLoggingService
                        .findAccountByLogin(login)
                        .ifPresent(account -> {

                            String ip = getClientIp();

                            loginAttemptLoggingService
                                    .recordFailedLogin(account, ip);

                            log.warn(
                                    "Failed login attempt for user '{}' from IP: {}",
                                    login,
                                    ip
                            );
                        });

            } else {
                log.warn(
                        "Failed login attempt, but could not extract login"
                );
            }

        } catch (Exception e) {

            log.error(
                    "FailedLoginInterceptor failed",
                    e
            );

        }
    }

    private String extractLoginFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof LoginRequestDTO dto) {
                return dto.login();
            }
            if (arg instanceof TwoFARequestDTO dto) {
                return loginAttemptLoggingService.getLoginFromTwoFARequest(dto).orElse(null);
            }
            if (arg instanceof LoginCodeDTO dto) {
                return loginAttemptLoggingService.getLoginFromLoginCode(dto).orElse(null);
            }
        }
        return null;
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
