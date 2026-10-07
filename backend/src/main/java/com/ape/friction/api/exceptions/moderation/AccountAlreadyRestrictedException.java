package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class AccountAlreadyRestrictedException extends AppBaseException {
    public AccountAlreadyRestrictedException() {
        super(HttpStatus.CONFLICT, "Account already has an active restriction");
    }
}
