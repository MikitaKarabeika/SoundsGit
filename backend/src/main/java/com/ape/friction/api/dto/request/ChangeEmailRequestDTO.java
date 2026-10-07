package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import com.ape.friction.api.dto.signature.Signable;

import java.util.Map;
import java.util.UUID;

@Builder
public record ChangeEmailRequestDTO(
        @NotNull
        long version,

        @NotNull
        UUID id,

        @NotBlank
        @Email
        @Size(max = 128)
        String currentEmail,

        @NotBlank
        @Email
        @Size(max = 128)
        String newEmail
) implements Signable {
    @Override
    public Map<String, Object> getSignableProperties () {
        return Map.of("id", id.toString(), "version", version);
    }

}
