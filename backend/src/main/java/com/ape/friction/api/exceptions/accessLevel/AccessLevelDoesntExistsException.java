package com.ape.friction.api.exceptions.accessLevel;

import org.springframework.http.HttpStatus;import com.ape.friction.api.exceptions.AppBaseException;

public class AccessLevelDoesntExistsException extends AppBaseException {
    public AccessLevelDoesntExistsException() {
        super(HttpStatus.NOT_FOUND, "Access level doesnt exist");
    }
}
