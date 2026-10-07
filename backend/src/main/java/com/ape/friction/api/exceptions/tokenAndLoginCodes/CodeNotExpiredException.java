package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class CodeNotExpiredException extends AppBaseException {
    public CodeNotExpiredException() {
        super(HttpStatus.CONFLICT,"Previous code is not expired, please use the previous one or try again until it will be expired (5 minutes after receiving email)");
    }
}
