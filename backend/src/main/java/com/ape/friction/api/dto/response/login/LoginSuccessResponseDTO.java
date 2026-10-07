package com.ape.friction.api.dto.response.login;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginSuccessResponseDTO implements LoginResponseDTO {
    private boolean requires2FA = false;
    @NotBlank
    private String accessToken;
    @NotBlank
    private String refreshToken;
}
