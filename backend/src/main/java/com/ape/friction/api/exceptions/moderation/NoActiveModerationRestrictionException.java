package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class NoActiveModerationRestrictionException extends AppBaseException {
    public NoActiveModerationRestrictionException() {
        super(HttpStatus.CONFLICT, "Account has no active moderation restriction");
    }
}
