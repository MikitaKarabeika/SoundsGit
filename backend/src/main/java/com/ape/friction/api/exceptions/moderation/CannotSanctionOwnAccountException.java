package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class CannotSanctionOwnAccountException extends AppBaseException {
    public CannotSanctionOwnAccountException() {
        super(HttpStatus.BAD_REQUEST, "You cannot sanction your own account");
    }
}
