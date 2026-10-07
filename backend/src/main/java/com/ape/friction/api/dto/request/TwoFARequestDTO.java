package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TwoFARequestDTO(
        @NotNull
        UUID authSessionId,

        @NotBlank
        @Pattern(regexp = "^\\d{8}$")
        String token
) {

}
