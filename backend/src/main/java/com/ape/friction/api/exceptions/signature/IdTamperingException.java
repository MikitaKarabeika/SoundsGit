package com.ape.friction.api.exceptions.signature;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class IdTamperingException extends AppBaseException {
    public IdTamperingException() { super(HttpStatus.FORBIDDEN, "ID tampering detected");}
}
