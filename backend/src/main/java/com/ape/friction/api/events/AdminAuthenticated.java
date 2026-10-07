package com.ape.friction.api.events;

import java.util.UUID;

public record AdminAuthenticated(
            UUID id,
            String ip
) {
}
