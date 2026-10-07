package com.ape.friction.api.exceptions.accessLevel;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class AdminCannotRevokeHisOwnAdminRole extends AppBaseException {
    public AdminCannotRevokeHisOwnAdminRole() {
        super(HttpStatus.BAD_REQUEST, "Admin cannot revoke his own admin role");
    }
}
