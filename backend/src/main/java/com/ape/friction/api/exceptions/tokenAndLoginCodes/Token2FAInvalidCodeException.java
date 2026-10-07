package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class Token2FAInvalidCodeException extends AppBaseException {
    public Token2FAInvalidCodeException() {
        super(HttpStatus.BAD_REQUEST, "Invalid 2FA code");
    }
}
