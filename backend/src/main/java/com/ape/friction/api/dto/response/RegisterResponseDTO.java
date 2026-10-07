package com.ape.friction.api.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RegisterResponseDTO(
        @NotBlank
        String message
)
{

}
