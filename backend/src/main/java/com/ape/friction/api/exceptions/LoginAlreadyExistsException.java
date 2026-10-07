package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class LoginAlreadyExistsException extends AppBaseException {
    public LoginAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Login already exists");
    }
}
