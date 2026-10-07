package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterRequestDTO(
        @NotBlank
        @Size(min = 1, max = 32)
        String name,

        @NotBlank
        @Size(min = 1, max = 32)
        String surname,

        @NotBlank
        @Email
        @Size(max = 128)
        String email,

        @NotBlank
        @Size(min = 3, max = 32)
        String login,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,64}$"
        )
        String password,

        String captchaToken

) {

}
