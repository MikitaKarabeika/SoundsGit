package com.ape.friction.api.dto.response.login;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginCodeResponseDTO implements LoginResponseDTO {

    @NotNull
    private UUID authSessionId;
}
