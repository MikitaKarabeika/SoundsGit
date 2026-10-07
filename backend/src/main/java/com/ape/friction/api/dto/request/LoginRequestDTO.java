package com.ape.friction.api.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LoginRequestDTO(
        @NotBlank
        @Size(min = 3, max = 32)
        String login,

        @NotBlank
        String password,

        String captchaToken
) {


}
