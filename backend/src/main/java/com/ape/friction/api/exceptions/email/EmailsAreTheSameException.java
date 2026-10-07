package com.ape.friction.api.exceptions.email;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class EmailsAreTheSameException extends AppBaseException {
    public EmailsAreTheSameException() {
        super(HttpStatus.BAD_REQUEST, "Emails are the same");
    }
}
