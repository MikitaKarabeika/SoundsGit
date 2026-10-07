package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class BadLoginCredentialsException extends AppBaseException {
    public BadLoginCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Bad Login Credentials");
    }
}
