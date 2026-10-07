package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class NewEmailSameAsOldException extends AppBaseException {

    public NewEmailSameAsOldException() {
        super(HttpStatus.BAD_REQUEST, "New email must be different from current email");
    }
}
