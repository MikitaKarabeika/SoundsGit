package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;


public class NotAvailableAccessLevelException extends AppBaseException {
    public NotAvailableAccessLevelException(String message) {
        super(HttpStatus.NOT_FOUND ,"Not available access level " + message);
    }
}
