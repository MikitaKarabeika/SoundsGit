package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class Token2FANotFoundException extends AppBaseException {
    public Token2FANotFoundException() {
        super(HttpStatus.NOT_FOUND, "Token 2fa does not exist");
    }
}
