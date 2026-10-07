package com.ape.friction.api.interceptors;

import java.util.Map;
import java.util.UUID;

public record SignedData(UUID id, Long version, Map<String, Object> allClaims) {
}
