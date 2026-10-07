package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;


@Builder
public record ViewChangeAccessLevelRequestDTO(
        @NotBlank
        @Size(min = 1, max = 32)
        @Pattern(regexp = "CLIENT|ADMIN|MODERATOR", flags = Pattern.Flag.CASE_INSENSITIVE)
        String chosenViewLevelName
) {

}

