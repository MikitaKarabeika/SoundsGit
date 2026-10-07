package com.ape.friction.api.exceptions.stripe;

import org.springframework.http.HttpStatusCode;
import com.ape.friction.api.exceptions.AppBaseException;

public class StripeSignatureVerificationException extends AppBaseException {
    public StripeSignatureVerificationException(HttpStatusCode status, String reason) {
        super(status, reason);
    }
}
