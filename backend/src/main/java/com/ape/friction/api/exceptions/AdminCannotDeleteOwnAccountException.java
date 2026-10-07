package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class AdminCannotDeleteOwnAccountException extends AppBaseException {
    public AdminCannotDeleteOwnAccountException() {
        super(HttpStatus.FORBIDDEN, "Admin cannot delete own account");
    }
}
