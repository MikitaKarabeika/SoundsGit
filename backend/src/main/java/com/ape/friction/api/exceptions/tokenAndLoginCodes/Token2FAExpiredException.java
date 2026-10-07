package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class Token2FAExpiredException extends AppBaseException {
    public Token2FAExpiredException(){
        super(HttpStatus.NOT_FOUND, "Token has expired");
    }
}
