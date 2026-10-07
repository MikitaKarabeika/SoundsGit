package com.ape.friction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class LoginAuditDTO {
    private Instant lastLoginAt;
    private String lastSuccessfulAuthIp;
    private Instant lastFailedAuthTime;
    private String lastFailedAuthIp;
}
