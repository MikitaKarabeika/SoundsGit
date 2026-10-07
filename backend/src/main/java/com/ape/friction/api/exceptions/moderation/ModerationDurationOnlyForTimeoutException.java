package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class ModerationDurationOnlyForTimeoutException extends AppBaseException {
    public ModerationDurationOnlyForTimeoutException() {
        super(HttpStatus.BAD_REQUEST, "Duration is only valid for a timeout");
    }
}
