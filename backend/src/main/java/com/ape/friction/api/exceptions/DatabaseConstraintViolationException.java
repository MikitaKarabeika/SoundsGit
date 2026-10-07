package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class DatabaseConstraintViolationException extends AppBaseException {
    public DatabaseConstraintViolationException(String constraintName) {
        super(HttpStatus.CONFLICT, "Database constraint violation: " + constraintName);
    }
}
