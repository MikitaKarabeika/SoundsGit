package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class DetailsOfAccountDontMatchException extends AppBaseException {
    public DetailsOfAccountDontMatchException() {
        super(HttpStatus.CONFLICT, "Details of Account Dont Match");
    }
}
