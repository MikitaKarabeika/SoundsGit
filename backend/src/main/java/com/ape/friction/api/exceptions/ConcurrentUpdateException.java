package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class ConcurrentUpdateException extends AppBaseException {

    public ConcurrentUpdateException(Throwable cause) {
        super(HttpStatus.CONFLICT, "object modified since read for update", cause);
    }

    public ConcurrentUpdateException() {
        super(HttpStatus.CONFLICT, "object modified since read for update");
    }
}
