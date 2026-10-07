package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class AccountConstraintViolationException extends AppBaseException{
    public AccountConstraintViolationException(String key) { // key will be sent to the client; or might it be full message?
        super(HttpStatus.CONFLICT, key);
    }
    public AccountConstraintViolationException() {
        super(HttpStatus.CONFLICT, "Unspecified account constraint violation");
    }
    public AccountConstraintViolationException(Throwable cause) {
        super(HttpStatus.CONFLICT, "Unspecified account constraint violation", cause);
    }

}
