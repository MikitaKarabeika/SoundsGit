package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class NotVerifiedAccountException extends AppBaseException {
    public NotVerifiedAccountException() {
        super(HttpStatus.FORBIDDEN, "Account Not Verified");
    }
}
