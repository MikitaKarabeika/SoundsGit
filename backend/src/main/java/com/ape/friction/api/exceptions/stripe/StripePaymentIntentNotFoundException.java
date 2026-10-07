package com.ape.friction.api.exceptions.stripe;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class StripePaymentIntentNotFoundException extends AppBaseException {
    public StripePaymentIntentNotFoundException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe Payment Intent Not Found Exception");
    }
}
