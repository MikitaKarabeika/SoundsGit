package com.ape.friction.api.interceptors;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.ape.friction.api.exceptions.DatabaseConstraintViolationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Aspect @Order(Ordered.LOWEST_PRECEDENCE-100) // So that it's "external" compared to logging interceptor
@Component
public class ConstraintViolationsHandlingInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ConstraintViolationsHandlingInterceptor.class);

    @AfterThrowing(pointcut = "Pointcuts.allRepositoryMethods()", throwing = "dive")
    public void handleDataIntegrityViolationException(DataIntegrityViolationException dive) {
        String constraintName = extractConstraintName(dive);
        log.info("Exception caught in AccountConstraintViolationsHandlingInterceptor" +  constraintName + " caused by " + dive);
        throw new DatabaseConstraintViolationException(constraintName);
    }

    private String extractConstraintName(DataIntegrityViolationException dive) {
        Throwable cause = dive.getMostSpecificCause();
        String message = (cause != null) ? cause.getMessage() : dive.getMessage();

        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown_constraint";
    }
}

