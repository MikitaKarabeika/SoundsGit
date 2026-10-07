package com.ape.friction.api.filters.userPrincipal;

import java.security.Principal;
import java.util.UUID;

public record UserPrincipal(UUID id, String login) implements Principal {
    @Override
    public String getName() {
        return login;
    }

    public  UUID getId() {
        return id;
    }
}
