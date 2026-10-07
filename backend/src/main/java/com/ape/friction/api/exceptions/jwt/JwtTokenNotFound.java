package com.ape.friction.api.exceptions.jwt;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class JwtTokenNotFound extends AppBaseException {
    public JwtTokenNotFound() {super(HttpStatus.UNAUTHORIZED, "Token not found"); }
}
