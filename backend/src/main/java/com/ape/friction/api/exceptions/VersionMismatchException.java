package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class VersionMismatchException extends AppBaseException {
    public VersionMismatchException() {
        super(HttpStatus.CONFLICT, "object modified since read for update");
    }

}
