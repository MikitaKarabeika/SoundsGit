package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class AccountAlreadyPermanentlyBannedException extends AppBaseException {
    public AccountAlreadyPermanentlyBannedException() {
        super(HttpStatus.CONFLICT, "Account is already permanently banned");
    }
}
