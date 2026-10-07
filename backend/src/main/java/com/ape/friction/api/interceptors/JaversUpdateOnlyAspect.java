package com.ape.friction.api.interceptors;

import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Hibernate;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.type.EntityType;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class JaversUpdateOnlyAspect {

    private final Javers javers;

    @AfterReturning(
            pointcut = "@within(javersUpdateRepository)",
            returning = "result"
    )
    public void auditRepositoryUpdate(JoinPoint joinPoint, Object result, JaversUpdateRepository javersUpdateRepository) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("checkpoint1");
        System.out.println(methodName);
        if (methodName.startsWith("saveAndFlush")) {
            System.out.println("checkpoint1.5");
            Object entityToSave = (result != null) ? result : getEntityFromArgs(joinPoint.getArgs());
            System.out.println("checkpoint2");
            if (entityToSave != null) {
                System.out.println("checkpoint3");
                Object realEntity = Hibernate.unproxy(entityToSave);
                if (isUpdateOperation(realEntity)) {
                    System.out.println("checkpoint4");
                    commitToJavers(realEntity);
                }
            }
            return;
        }

        // delete methods
        if (methodName.startsWith("delete")) {
            handleDelete(joinPoint.getArgs());
            return;
        }

        // custom @Modifying queries (example)
        if (methodName.equals("updateAccountStatus")) {
            log.debug("@Modifying query method intercepted, manual audit might be required.");
        }
    }

    private void handleDelete(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;
            if (isEntity(arg)) {
                Object realEntity = Hibernate.unproxy(arg);
                commitDelete(realEntity);
            } else if (arg instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (isEntity(item)) {
                        Object realEntity = Hibernate.unproxy(item);
                        commitDelete(realEntity);
                    }
                }
            } else if (isIdType(arg)) {
                log.warn("deleteById with ID {} intercepted, but entity class unknown. Audit may be incomplete.", arg);
            }
        }
    }

    private boolean isEntity(Object obj) {
        return obj != null && obj.getClass().isAnnotationPresent(Entity.class);
    }

    private boolean isIdType(Object obj) {
        return obj instanceof UUID || obj instanceof Long || obj instanceof Integer || obj instanceof String;
    }

    private Object getEntityFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg != null && !arg.getClass().isArray() && !(arg instanceof Iterable)) {
                return arg;
            }
        }
        return null;
    }

    private boolean isUpdateOperation(Object entity) {
        try {
            Class<?> entityClass = ClassUtils.getUserClass(Hibernate.unproxy(entity));

            EntityType entityType = javers.getTypeMapping(entityClass);
            Object id = entityType.getIdOf(entity);
            if (id == null) return false;

            List<CdoSnapshot> snapshots = javers.findSnapshots(
                    QueryBuilder.byInstanceId(id, entityClass).build()
            );
            return !snapshots.isEmpty();
        } catch (Exception e) {
            log.error("Failed to determine if entity is update operation", e);
            return false;
        }
    }

    private void commitToJavers(Object entity) {
        String author = getCurrentAuthor();
        try {
            javers.commit(author, entity);
            log.debug("Audit committed for update of {} by {}", entity.getClass().getSimpleName(), author);
        } catch (Exception e) {
            log.error("Failed to commit audit for update of {}: {}", entity.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    private void commitDelete(Object entity) {
        String author = getCurrentAuthor();
        try {
            javers.commitShallowDelete(author, entity);
            log.debug("Delete audit committed for {} by {}", entity.getClass().getSimpleName(), author);
        } catch (Exception e) {
            log.error("Failed to commit delete audit for {}: {}", entity.getClass().getSimpleName(), e.getMessage(), e);
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
