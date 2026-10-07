package com.ape.friction.api.dto.request;

import lombok.Builder;

@Builder
public record RefreshRequest(
        String refreshToken) {

}
