package com.ape.friction.api.dto.response;

import com.ape.friction.api.entities.moderation.ModerationOutcome;
import com.ape.friction.api.entities.moderation.ModerationSanction;

import java.time.Instant;
import java.util.UUID;

public record ModerationDecisionResponseDTO(
        UUID id,
        UUID reportId,
        String reportedLogin,
        String reporterLogin,
        String moderatorLogin,
        ModerationOutcome outcome,
        String note,
        Instant decidedAt,
        ModerationSanction sanction,
        Instant sanctionExpiresAt
) {
}
