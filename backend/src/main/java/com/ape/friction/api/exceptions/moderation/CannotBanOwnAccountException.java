package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class CannotBanOwnAccountException extends AppBaseException {
    public CannotBanOwnAccountException() {
        super(HttpStatus.BAD_REQUEST, "You cannot ban your own account");
    }
}
