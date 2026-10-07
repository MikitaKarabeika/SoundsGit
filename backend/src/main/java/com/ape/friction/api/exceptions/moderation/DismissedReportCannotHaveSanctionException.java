package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class DismissedReportCannotHaveSanctionException extends AppBaseException {
    public DismissedReportCannotHaveSanctionException() {
        super(HttpStatus.BAD_REQUEST, "Dismissed reports cannot include a sanction");
    }
}
