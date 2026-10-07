package com.ape.friction.api.exceptions.accessLevel;

import org.springframework.http.HttpStatus;import com.ape.friction.api.exceptions.AppBaseException;public class AccessLevelTheSameException extends AppBaseException {
    public AccessLevelTheSameException() {
        super(HttpStatus.CONFLICT, "Access level status is the Same");
    }
}
