package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

// S2_KODYJEDN — body dla PATCH /accounts/me/two-factor-auth
@Builder
public record TwoFaToggleRequestDTO(
        @NotNull
        Boolean enabled
) {


}
