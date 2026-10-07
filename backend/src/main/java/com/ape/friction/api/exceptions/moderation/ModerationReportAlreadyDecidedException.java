package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class ModerationReportAlreadyDecidedException extends AppBaseException {
    public ModerationReportAlreadyDecidedException() {
        super(HttpStatus.CONFLICT, "Moderation report has already been decided");
    }
}
