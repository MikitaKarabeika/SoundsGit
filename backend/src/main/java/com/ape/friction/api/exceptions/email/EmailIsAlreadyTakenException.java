package com.ape.friction.api.exceptions.email;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class EmailIsAlreadyTakenException extends AppBaseException {
    public EmailIsAlreadyTakenException() {
        super(HttpStatus.CONFLICT, "Email is already taken by other account");
    }
}
