package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class ItIsYourAccountException extends AppBaseException {
    public ItIsYourAccountException() {
        super(HttpStatus.FORBIDDEN, "It is your account, choose other method");
    }
}
