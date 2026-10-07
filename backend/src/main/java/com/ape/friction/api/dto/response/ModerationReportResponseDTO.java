package com.ape.friction.api.dto.response;

import com.ape.friction.api.entities.moderation.ReportCategory;

import java.time.Instant;
import java.util.UUID;

public record ModerationReportResponseDTO(
        UUID id,
        UUID reporterAccountId,
        String reporterLogin,
        UUID reportedAccountId,
        String reportedLogin,
        ReportCategory category,
        String details,
        Instant createdAt
) {
}
