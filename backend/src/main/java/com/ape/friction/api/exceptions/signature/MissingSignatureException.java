package com.ape.friction.api.exceptions.signature;


import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class MissingSignatureException extends AppBaseException {
    public MissingSignatureException() {super(HttpStatus.BAD_REQUEST, "Missing Signature");}
}
