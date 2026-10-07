package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidOldPasswordException extends AppBaseException {

    public InvalidOldPasswordException() {
        super(HttpStatus.BAD_REQUEST, "Old password is incorrect");
    }
}
