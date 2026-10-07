package com.ape.friction.api.dto.internal;

import java.math.BigDecimal;

public record PayPalCaptureResult(
        String status,
        String captureId,
        BigDecimal amount
) {
}
