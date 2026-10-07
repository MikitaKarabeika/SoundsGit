package com.ape.friction.api.exceptions.stripe;

import org.springframework.http.HttpStatusCode;
import com.ape.friction.api.exceptions.AppBaseException;

public class StripeApiException extends AppBaseException {
    public StripeApiException(HttpStatusCode status, String reason) {
        super(status, reason);
    }
}
