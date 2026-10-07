package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record LoginCodeDTO(
        @NotNull
        UUID authSessionId,

        @NotBlank
        @Size(min=10, max=10)
        String code
) { }
