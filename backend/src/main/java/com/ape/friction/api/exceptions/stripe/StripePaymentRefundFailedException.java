package com.ape.friction.api.exceptions.stripe;

import org.springframework.http.HttpStatus;
import com.ape.friction.api.exceptions.AppBaseException;

public class StripePaymentRefundFailedException extends AppBaseException {
    public StripePaymentRefundFailedException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe Payment Refund Failed");
    }
}
