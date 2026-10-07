package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class PasswordUsedInPastException extends AppBaseException {
    public PasswordUsedInPastException() {
        super(HttpStatus.BAD_REQUEST, "New password can't match one used in Past");
    }
}
