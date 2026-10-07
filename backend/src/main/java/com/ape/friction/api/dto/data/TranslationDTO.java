package com.ape.friction.api.dto.data;

import lombok.Builder;
import com.ape.friction.api.common.Language;

@Builder
public record TranslationDTO(
        Language language,
        String name
) { }
