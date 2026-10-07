package com.ape.friction.api.exceptions.email;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class AlreadyRequestedToChangeEmailException extends AppBaseException {
    public AlreadyRequestedToChangeEmailException() {
        super(HttpStatus.CONFLICT, "You already requested to change your email.");
    }
}
