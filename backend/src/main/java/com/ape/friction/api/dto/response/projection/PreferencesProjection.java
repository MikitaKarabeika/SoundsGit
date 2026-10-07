package com.ape.friction.api.dto.response.projection;

import com.ape.friction.api.common.Language;
import com.ape.friction.api.entities.users.Theme;

public interface PreferencesProjection {

    Language getLanguage();
    Theme getTheme();
}
