package com.ape.friction.api.interceptors;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import com.ape.friction.api.exceptions.ConcurrentUpdateException;


@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100) // So that it's "external" compared to logging interceptor
@Component
public class GenericOptimisticLockHandlingInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GenericOptimisticLockHandlingInterceptor.class);

    @AfterThrowing(pointcut = "Pointcuts.allRepositoryMethods()", throwing = "olfe")
    public void handleOptimisticLockException(JoinPoint joinPoint, OptimisticLockingFailureException olfe) {
        throw new ConcurrentUpdateException(olfe);
    }

}