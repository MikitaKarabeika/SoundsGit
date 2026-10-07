package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class InvalidModerationTimeoutException extends AppBaseException {
    public InvalidModerationTimeoutException() {
        super(HttpStatus.BAD_REQUEST, "Timeout must be between 1 and 168 hours");
    }
}
