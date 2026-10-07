package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.ape.friction.api.entities.moderation.ModerationOutcome;
import com.ape.friction.api.entities.moderation.ModerationSanction;

public record ModerationDecisionRequestDTO(
        @NotNull ModerationOutcome outcome,
        @NotBlank @Size(max = 1000) String note,
        ModerationSanction sanction,
        @Min(1) @Max(168) Integer durationHours
) {
}
