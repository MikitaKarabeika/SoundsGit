package com.ape.friction.api.dto.response.projection;

import java.time.Instant;

public interface LoginAuditProjection {
    Instant getLastLoginAt();
    String getLastSuccessfulAuthIp();
    Instant getLastFailedAuthTime();
    String getLastFailedAuthIp();
}
