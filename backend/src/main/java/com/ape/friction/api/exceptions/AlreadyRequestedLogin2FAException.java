package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class AlreadyRequestedLogin2FAException extends AppBaseException {
    public AlreadyRequestedLogin2FAException() {
        super(HttpStatus.CONFLICT, "2FA code already requested for this account");
    }
}
