package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthenticatedUserException extends AppBaseException {
    public UnauthenticatedUserException() {
        super(HttpStatus.UNAUTHORIZED, "User not Authenticated");
    }
}
