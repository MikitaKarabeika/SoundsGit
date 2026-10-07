package com.ape.friction.api.dto.data;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import com.ape.friction.api.common.Language;
import com.ape.friction.api.entities.users.Theme;


@Builder
public record PreferencesDataDTO(

        @Enumerated(EnumType.STRING)
        Language language,

        @Enumerated(EnumType.STRING)
        Theme  theme

) {
}
