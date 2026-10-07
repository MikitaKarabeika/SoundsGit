package com.ape.friction.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import com.ape.friction.api.common.validation.ValidTimezone;

@Builder
public record EditTimeZoneDataDTO(
        @NotBlank
        @Size(min = 1, max = 32)
        @ValidTimezone
        String timezone
) {

}
