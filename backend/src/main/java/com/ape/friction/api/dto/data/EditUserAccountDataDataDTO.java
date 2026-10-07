package com.ape.friction.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import com.ape.friction.api.dto.signature.Signable;

import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
public record EditUserAccountDataDataDTO (
        @NotBlank
        @Size(min = 1, max = 32)
        String name,

        @NotNull
        long version,

        UUID id,

        @NotBlank
        @Size(min = 1, max = 32)
        String surname


        ) implements Signable {
    @Override
    public Map<String, Object> getSignableProperties() {
        return Map.of(
                "id", id.toString(),
                "version", version
        );
    }
}
