package com.ape.friction.api.dto.data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;


@Builder
public record ForgotPasswordDataDTO(
        @NotBlank
        @Email
        @Size(max = 128)
        String email
) {

}
