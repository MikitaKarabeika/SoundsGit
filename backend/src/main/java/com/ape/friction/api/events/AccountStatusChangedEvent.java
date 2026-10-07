package com.ape.friction.api.events;


import java.util.UUID;


public record AccountStatusChangedEvent(
        UUID id,
        String cause) {
}
