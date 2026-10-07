package com.ape.friction.api.dto.response;

import com.ape.friction.api.entities.moderation.ModerationAccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountModerationStatusDTO(
        UUID accountId,
        String login,
        ModerationAccountStatus moderationStatus,
        String note,
        String actionBy,
        Instant changedAt,
        Instant expiresAt,
        UUID reportId
) {
}
