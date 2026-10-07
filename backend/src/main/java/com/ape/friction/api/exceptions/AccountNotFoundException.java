package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends AppBaseException{
    public AccountNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Account not found");
    }
}
