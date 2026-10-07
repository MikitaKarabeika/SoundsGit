package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class CaptchaVerificationException extends AppBaseException {
    public CaptchaVerificationException() {
        super(HttpStatus.BAD_REQUEST, "Captcha verification failed");
    }
}
