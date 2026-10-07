package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;


public class ChangeOwnPasswordTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void shouldChangeOwnPassword() {
        String login = "spiderman";
        String oldPassword = "Password1!";
        String newPassword = "NewPassword1!";

        String token = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", oldPassword
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();

        restTestClient.patch()
                .uri("/accounts/me/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "oldPassword", oldPassword,
                        "newPassword", newPassword,
                        "repeatedNewPassword", newPassword
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        String tokenAfterChange = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", newPassword
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();


    }

    @Test
    void shouldRejectWhenOldPasswordIsWrong() {
        String login = "batman";
        String originalPassword = "Password1!";

        String token = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", originalPassword
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();

        restTestClient.patch()
                .uri("/accounts/me/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "oldPassword", "WrongPassword1!",
                        "newPassword", "NewPassword1!",
                        "repeatedNewPassword", "NewPassword1!"
                ))
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", originalPassword
                ))
                .exchange()
                .expectStatus().isOk();
    }


    @Test
    void shouldChangeOwnPasswordAndAllowLoginOnlyWithNewPassword() {
        String login = "ironman";
        String oldPassword = "Password1!";
        String newPassword = "NewPassword1!";

        String token = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", oldPassword
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();

        restTestClient.patch()
                .uri("/accounts/me/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "oldPassword", oldPassword,
                        "newPassword", newPassword,
                        "repeatedNewPassword", newPassword
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", oldPassword
                ))
                .exchange()
                .expectStatus().isUnauthorized();

        String tokenAfterChange = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", newPassword
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();


    }
}
