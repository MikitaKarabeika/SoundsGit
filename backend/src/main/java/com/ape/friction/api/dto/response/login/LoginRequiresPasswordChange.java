package com.ape.friction.api.dto.response.login;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ape.friction.api.dto.signature.Signable;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequiresPasswordChange implements LoginResponseDTO, Signable {

    @NotNull
    private UUID id;

    @NotNull
    private long version;

    @Override
    public Map<String, Object> getSignableProperties() {
        return Map.of("id", id.toString(), "version", version);
    }
}
