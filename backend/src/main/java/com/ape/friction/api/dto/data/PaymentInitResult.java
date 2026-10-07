package com.ape.friction.api.dto.data;

public record PaymentInitResult(
        String paymentId,
        String approvalUrl,
        String status
) {}
