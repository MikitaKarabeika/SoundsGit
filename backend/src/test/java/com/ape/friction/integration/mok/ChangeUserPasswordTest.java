// MOK.14 Zmień hasło innego Użytkownika

package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;
import java.util.UUID;


public class ChangeUserPasswordTest extends MOKIntegrationTestBase {

    @LocalServerPort
    private int port;

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
                .get("accessToken").toString();
    }

    private UUID createTestUser(String adminToken, String login, String email) {
        var addUserRequest = Map.of(
                "name", "Test",
                "surname", "User",
                "email", email,
                "login", login,
                "password", "InitialPass1!",
                "accessLevels", java.util.List.of(Map.of("levelName", "CLIENT"))
        );

        Map response = restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addUserRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        return UUID.fromString(response.get("id").toString());
    }

    @Test
    void ShouldChangeUserPasswordAsAdmin() {
        String adminToken = loginAsAdmin();
        UUID userId = createTestUser(adminToken, "chgpwduserone", "chgpwduserone@example.com");

        var request = Map.of(
                "newPassword", "NewPassword1!"
        );

        restTestClient.post()
                .uri("/accounts/" + userId + "/change-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk();

        var loginRequest = Map.of(
                "login", "chgpwduserone",
                "password", "NewPassword1!"
        );

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void ShouldFailWhenNewPasswordIsWeak() {
        String adminToken = loginAsAdmin();
        UUID userId = createTestUser(adminToken, "chgpwdusertwo", "chgpwdusertwo@example.com");

        var request = Map.of(
                "newPassword", "weakpassword"
        );

        restTestClient.post()
                .uri("/accounts/" + userId + "/change-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenNewPasswordIsBlank() {
        String adminToken = loginAsAdmin();
        UUID userId = createTestUser(adminToken, "chgpwduserthree", "chgpwduserthree@example.com");

        var request = Map.of(
                "newPassword", ""
        );

        restTestClient.post()
                .uri("/accounts/" + userId + "/change-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenTargetAccountNotFound() {
        String adminToken = loginAsAdmin();
        UUID nonExistentId = UUID.randomUUID();

        var request = Map.of(
                "newPassword", "NewPassword1!"
        );

        restTestClient.post()
                .uri("/accounts/" + nonExistentId + "/change-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void ShouldFailWhenCalledWithoutAuthentication() {
        UUID randomId = UUID.randomUUID();

        var request = Map.of(
                "newPassword", "NewPassword1!"
        );

        restTestClient.post()
                .uri("/accounts/" + randomId + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
