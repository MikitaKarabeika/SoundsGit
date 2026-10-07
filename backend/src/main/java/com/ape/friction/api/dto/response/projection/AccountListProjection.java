package com.ape.friction.api.dto.response.projection;


import java.util.Collection;
import java.util.UUID;

public interface AccountListProjection {
    UUID getId();
    String getLogin();
    String getName();
    String getSurname();
    String getEmail();
    boolean isActive();
    boolean isVerified();

    Collection<AccessLevelProjection> getAccessLevels();
}
