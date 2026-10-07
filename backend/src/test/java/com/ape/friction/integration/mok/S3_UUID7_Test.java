package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class S3_UUID7_Test extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    private String loginAsAdmin() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();
    }

    private Map<String, Object> validRequest(String email, String login) {
        return Map.of(
                "name", "Jan",
                "surname", "Kowalski",
                "email", email,
                "login", login,
                "password", "Password1!",
                "accessLevels", List.of(Map.of("levelName", "CLIENT"))
        );
    }

    private Map createAccount(String adminToken, String email, String login) {
        return restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest(email, login))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void ShouldGenerateUUID7WhenAccountIsCreated() {
        String adminToken = loginAsAdmin();

        Map response = createAccount(
                adminToken,
                "uuid7-created@example.com",
                "uuid7created"
        );

        assertNotNull(response);
        assertNotNull(response.get("id"));

        UUID generatedId = UUID.fromString(response.get("id").toString());

        assertEquals(7, generatedId.version());
        assertEquals(2, generatedId.variant());
    }

    @Test
    void ShouldGenerateUUID7WithCurrentTimestampWhenAccountIsCreated() {
        String adminToken = loginAsAdmin();

        long before = System.currentTimeMillis();

        Map response = createAccount(
                adminToken,
                "uuid7-timestamp@example.com",
                "uuid7timestamp"
        );

        long after = System.currentTimeMillis();

        assertNotNull(response);
        assertNotNull(response.get("id"));

        UUID generatedId = UUID.fromString(response.get("id").toString());
        long timestamp = extractTimestamp(generatedId);

        assertEquals(7, generatedId.version());
        assertEquals(2, generatedId.variant());
        assertTrue(timestamp >= before);
        assertTrue(timestamp <= after);
    }

    @Test
    void ShouldReturnPersistedAccountWithTheSameUUID7Id() {
        String adminToken = loginAsAdmin();

        Map createResponse = createAccount(
                adminToken,
                "uuid7-persisted@example.com",
                "uuid7persisted"
        );

        assertNotNull(createResponse);
        assertNotNull(createResponse.get("id"));

        String createdId = createResponse.get("id").toString();

        Map getResponse = restTestClient.get()
                .uri("/accounts/{id}", createdId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(getResponse);
        assertEquals(createdId, getResponse.get("id").toString());

        UUID persistedId = UUID.fromString(getResponse.get("id").toString());

        assertEquals(7, persistedId.version());
        assertEquals(2, persistedId.variant());
    }

    @Test
    void ShouldGenerateDifferentUUID7IdsForDifferentAccounts() {
        String adminToken = loginAsAdmin();

        Map firstResponse = createAccount(
                adminToken,
                "uuid7-first@example.com",
                "uuid7first"
        );

        Map secondResponse = createAccount(
                adminToken,
                "uuid7-second@example.com",
                "uuid7second"
        );

        assertNotNull(firstResponse);
        assertNotNull(secondResponse);

        UUID firstId = UUID.fromString(firstResponse.get("id").toString());
        UUID secondId = UUID.fromString(secondResponse.get("id").toString());

        assertNotEquals(firstId, secondId);

        assertEquals(7, firstId.version());
        assertEquals(7, secondId.version());

        assertEquals(2, firstId.variant());
        assertEquals(2, secondId.variant());
    }

    private long extractTimestamp(UUID uuid) {
        return uuid.getMostSignificantBits() >>> 16;
    }
}
