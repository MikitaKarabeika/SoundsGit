package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationActionRequestDTO(
        @NotBlank @Size(max = 1000) String note
) {
}
