//package com.ape.friction.api.interceptors.rateLimiting;
//
//import io.github.resilience4j.ratelimiter.RateLimiter;
//import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
//import lombok.RequiredArgsConstructor;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import com.ape.friction.api.exceptions.UnauthenticatedUserException;
//import com.ape.friction.api.exceptions.survey.RateLimitExceededException;
//import com.ape.friction.api.filters.userPrincipal.UserPrincipal;
//
//@Aspect
//@Component
//@RequiredArgsConstructor
//public class UserRateLimitAspect {
//
//    private static final Logger log = LoggerFactory.getLogger(UserRateLimitAspect.class);
//
//    private final RateLimiterRegistry rateLimiterRegistry;
//
//    @Around("@annotation(userRateLimit)")
//    public Object enforceLimit(ProceedingJoinPoint joinPoint, UserRateLimit userRateLimit) throws Throwable {
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal userPrincipal)) {
//            throw new UnauthenticatedUserException();
//        }
//
//        String userId = userPrincipal.getId().toString();
//        String configName = userRateLimit.configName();
//
//        String instanceName = configName + "_" + userId;
//
//        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(instanceName, configName);
//
//        String message = "Warning, limit of using AI (while editing his survey more than 2 times) is exceeded by user: ";
//        String login = userPrincipal.getName();
//
//        if (!rateLimiter.acquirePermission()) {
//            log.info(message, login, userId);
//
//            throw new RateLimitExceededException();
//
//        }
//
//        return joinPoint.proceed();
//    }
//}
