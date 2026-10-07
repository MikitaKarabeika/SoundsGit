package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class TokenExpiredException extends AppBaseException {
    public TokenExpiredException() {
        super(HttpStatus.GONE,"Token is expired");
    }
}
