package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class CodeNotFoundException extends AppBaseException {
    public CodeNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Code does not exist");
    }
}

