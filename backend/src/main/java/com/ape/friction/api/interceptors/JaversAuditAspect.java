package com.ape.friction.api.interceptors;

import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Hibernate;
import org.javers.core.Javers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class JaversAuditAspect {

    private final Javers javers;

    @AfterReturning(
            pointcut = "@annotation(javersCreate)",
            returning = "result"
    )
    public void auditCreate(JoinPoint joinPoint, Object result, JaversCreate javersCreate) {
        audit(result, joinPoint);
    }

    private void audit(Object result, JoinPoint joinPoint) {
        String author = getCurrentAuthor();
        if (result instanceof Collection<?> collection) {
            for (Object item : collection) {
                commitIfEntity(item, author, joinPoint);
            }
        } else {
            commitIfEntity(result, author, joinPoint);
        }
    }

    private void commitIfEntity(Object obj, String author, JoinPoint joinPoint) {
        if (obj == null) return;

        Object realEntity;
        try {
            realEntity = Hibernate.unproxy(obj);
        } catch (Exception e) {
            log.error("Failed to unproxy entity for audit: {}", e.getMessage(), e);
            return;
        }

        if (realEntity.getClass().isAnnotationPresent(Entity.class)) {
            try {
                javers.commit(author, realEntity);
                log.debug("Audit committed for {} by {}", realEntity.getClass().getSimpleName(), author);
            } catch (Exception e) {
                log.error("Failed to commit audit for {}: {}", realEntity.getClass().getSimpleName(), e.getMessage(), e);
            }
        } else {
            log.warn("Method returned non-entity: {}. Audit skipped.", realEntity.getClass().getName());
        }
    }

    private String getCurrentAuthor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "system";
    }
}
