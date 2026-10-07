package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import com.ape.friction.api.dto.signature.Signable;

import java.util.Map;
import java.util.UUID;

@Builder
public record StripeCreateOrderRequestDTO(
        @NotNull
        UUID id,
        @NotNull Long version
) implements Signable {
    @Override
    public Map<String, Object> getSignableProperties () {
        return Map.of("id", id.toString(), "version", version);
    }


}
