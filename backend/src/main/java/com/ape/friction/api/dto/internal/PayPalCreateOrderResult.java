package com.ape.friction.api.dto.internal;

public record PayPalCreateOrderResult(
        String paypalOrderId,
        String approvalUrl
) {
}
