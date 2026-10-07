package com.ape.friction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * EXPMSG - Information about the current user's session lifetime, used by the
 * frontend to display a "session about to expire" warning and offer extension.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionInfoResponseDTO {
    /** When the access token was issued. */
    private Instant issuedAt;
    /** When the access token expires. */
    private Instant expiresAt;
    /** Server-now in epoch millis (helps client correct local clock drift). */
    private long serverTime;
    /** Seconds remaining until expiration (>= 0). 0 means expired. */
    private long secondsLeft;
    /** Threshold (in seconds) under which the UI should display the warning. */
    private long warningThresholdSeconds;
}
