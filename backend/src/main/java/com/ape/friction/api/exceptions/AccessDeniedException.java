package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends AppBaseException {
    public AccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "Access Denied");
    }
}
