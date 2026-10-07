package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class CodeExpiredException extends AppBaseException {
    public CodeExpiredException() {
        super(HttpStatus.BAD_REQUEST, "Code is expired, please login again and we will send the new one");
    }
}
