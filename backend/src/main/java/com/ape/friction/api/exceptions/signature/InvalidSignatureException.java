package com.ape.friction.api.exceptions.signature;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class InvalidSignatureException extends AppBaseException {
    public InvalidSignatureException() {super(HttpStatus.FORBIDDEN, "Invalid Signature");}
}
