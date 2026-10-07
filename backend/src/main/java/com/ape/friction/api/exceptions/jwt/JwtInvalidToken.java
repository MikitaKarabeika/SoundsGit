package com.ape.friction.api.exceptions.jwt;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class JwtInvalidToken extends AppBaseException {
    public JwtInvalidToken() {super(HttpStatus.UNAUTHORIZED, "Token not found"); }
}
