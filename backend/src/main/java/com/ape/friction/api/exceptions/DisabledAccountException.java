package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class DisabledAccountException extends AppBaseException {
    public DisabledAccountException() {
        super(HttpStatus.FORBIDDEN, "Your account has been disabled");
    }
}
