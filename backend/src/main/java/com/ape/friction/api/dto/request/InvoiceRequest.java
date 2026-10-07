package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Builder
public record InvoiceRequest(
        @NotNull
        UUID orderId,
        @NotNull
        BigDecimal price,
        @NotBlank
        String item,
        @NotBlank
        String clientName,
        @NotBlank
        String clientSurname,
        @NotBlank
        String deliveryAddress,
        @NotNull
        Locale locale

)
{ }
