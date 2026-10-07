package com.ape.friction.api.dto.data;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import com.ape.friction.api.common.Language;
import com.ape.friction.api.dto.signature.Signable;
import com.ape.friction.api.entities.users.Theme;

import java.util.Map;
import java.util.UUID;

@Builder
public record EditOwnAccountDataDTO (

        @NotNull
        UUID id,

        @NotBlank
        @Size(min = 1, max = 32)
        String name,

        @NotNull
        long version,

        @NotBlank
        @Size(min = 1, max = 32)
        String surname,

        @Enumerated(EnumType.STRING)
        Language language,

        @Enumerated(EnumType.STRING)
        Theme theme
) implements Signable {
    @Override
    public Map<String, Object> getSignableProperties() {
        return Map.of(
                "id", id.toString(),
                "version", version
        );
    }
}
