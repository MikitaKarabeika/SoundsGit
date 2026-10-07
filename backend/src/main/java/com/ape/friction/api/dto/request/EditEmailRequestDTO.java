package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;


@Builder
public record EditEmailRequestDTO(
        @NotBlank
        String token
) {
}
