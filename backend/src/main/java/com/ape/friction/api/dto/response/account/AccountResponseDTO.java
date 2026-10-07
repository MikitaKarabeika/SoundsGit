package com.ape.friction.api.dto.response.account;

import jakarta.persistence.Basic;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.ape.friction.api.common.Language;
import com.ape.friction.api.dto.signature.Signable;
import com.ape.friction.api.entities.users.Theme;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class AccountResponseDTO implements Signable {

    @NotBlank
    @Size(min = 1, max = 32)
    private String login;

    @NotBlank
    private Collection<AccessLevelDTO> accessLevels;

    @NotBlank
    private UUID id;

    @NotNull
    private long version;

    @NotBlank
    @Size(min = 1, max = 32)
    private String name;

    @NotBlank
    @Size(min = 1, max = 32)
    private String surname;

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @Enumerated(EnumType.STRING)
    private Language language;

    @Enumerated(EnumType.STRING)
    private Theme theme;

    @Basic(optional = false)
    private boolean verified;

    @Basic(optional = false)
    private boolean active;

    @Basic(optional = false)
    private boolean twoFaEnabled;

    @Override
    public Map<String, Object> getSignableProperties() {
        return Map.of("id", id.toString(), "version", version);
    }
}




