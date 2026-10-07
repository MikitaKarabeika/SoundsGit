package com.ape.friction.api.dto.data;

import java.time.Instant;
import java.util.List;

public record AuditRecordDTO(
        String author,
        Instant commitDate,
        String entityType,
        String changeType,
        List<String> changedProperties
) {}
