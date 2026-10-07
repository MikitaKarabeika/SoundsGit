package com.ape.friction.api.exceptions.moderation;

import com.ape.friction.api.exceptions.AppBaseException;
import org.springframework.http.HttpStatus;

public class ModeratorCannotSanctionAdministratorException extends AppBaseException {
    public ModeratorCannotSanctionAdministratorException() {
        super(HttpStatus.FORBIDDEN, "Moderators cannot sanction administrators");
    }
}
