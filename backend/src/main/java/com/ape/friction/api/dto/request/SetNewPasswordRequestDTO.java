package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import com.ape.friction.api.dto.signature.Signable;

import java.util.Map;
import java.util.UUID;


@Builder
public record SetNewPasswordRequestDTO (

        @NotNull
        UUID id,

        @NotNull
        long version,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,64}$"
        )
        String password
) implements Signable {
    @Override
    public Map<String, Object> getSignableProperties() {
        return Map.of("id", id.toString(), "version", version);
    }
}


