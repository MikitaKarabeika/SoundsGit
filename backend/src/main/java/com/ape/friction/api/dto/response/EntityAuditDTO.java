package com.ape.friction.api.dto.response;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import com.ape.friction.api.common.OperationType;

import java.time.Instant;

@Builder
public record EntityAuditDTO(

        Instant createdAt,

        String createdBy,

        Instant modifiedAt,

        String lastModifiedBy,

        @Enumerated(EnumType.STRING)
        OperationType lastOperationType


) {
}
