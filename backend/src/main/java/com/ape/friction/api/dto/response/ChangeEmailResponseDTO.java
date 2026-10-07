package com.ape.friction.api.dto.response;


import lombok.Builder;


@Builder
public record ChangeEmailResponseDTO(
        String message
) {

}
