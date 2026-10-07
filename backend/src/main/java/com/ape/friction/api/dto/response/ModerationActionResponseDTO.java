package com.ape.friction.api.dto.response;

import com.ape.friction.api.entities.moderation.ModerationActionType;

import java.time.Instant;
import java.util.UUID;

public record ModerationActionResponseDTO(
        UUID id,
        ModerationActionType actionType,
        String note,
        String actionBy,
        Instant changedAt,
        Instant expiresAt,
        UUID reportId
) {
}
