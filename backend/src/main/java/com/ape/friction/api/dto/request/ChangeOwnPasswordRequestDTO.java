package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ChangeOwnPasswordRequestDTO(
        @NotBlank
        String oldPassword,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,64}$"
        )
        String newPassword
) {


}
