package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class CannotReportOwnAccountException extends AppBaseException {
    public CannotReportOwnAccountException() {
        super(HttpStatus.BAD_REQUEST, "You cannot report your own account");
    }
}
