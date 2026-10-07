package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class AccountPermanentlyBannedException extends AppBaseException {
    public AccountPermanentlyBannedException() {
        super(HttpStatus.CONFLICT, "Account is permanently banned");
    }
}
