package com.ape.friction.api.exceptions.tokenAndLoginCodes;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class TokenNotFoundException extends AppBaseException {
  public TokenNotFoundException() {
    super(HttpStatus.NOT_FOUND, "Token does not exist");
  }
}
