package com.ape.friction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.ape.friction.api.entities.moderation.ReportCategory;

import java.util.UUID;

public record CreateModerationReportRequestDTO(
        @NotNull UUID reportedAccountId,
        @NotNull ReportCategory category,
        @NotBlank @Size(max = 2000) String details
) {
}
