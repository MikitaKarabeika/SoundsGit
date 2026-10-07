package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class ModerationReportNotFoundException extends AppBaseException {
    public ModerationReportNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Moderation report not found");
    }
}
