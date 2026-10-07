package com.ape.friction.api.exceptions;

import org.springframework.http.HttpStatus;

public class AdminCannotDeactivateOwnAccountException extends AppBaseException {
  public AdminCannotDeactivateOwnAccountException() {
    super(HttpStatus.FORBIDDEN, "Admin cannot deactivate own account");
  }
}
