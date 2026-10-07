package com.ape.friction.api.exceptions;


import org.springframework.http.HttpStatus;

public class BadPasswordException extends AppBaseException {
    public BadPasswordException() { super(HttpStatus.UNAUTHORIZED, "Bad Login Credentials"); }
}
