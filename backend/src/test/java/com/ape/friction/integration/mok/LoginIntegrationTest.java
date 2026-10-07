package com.ape.friction.integration.mok;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.request.RefreshRequest;
import   com.ape.friction.api.dto.response.login.LoginSuccessResponseDTO;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;


public class LoginIntegrationTest extends MOKIntegrationTestBase {


    @Autowired
    private RestTestClient restTestClient;

    @Test
    void shouldReturnTokensWhenLoginSuccessful() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.refreshToken").exists();
    }


    @Test
    void shouldReturnUnauthorizedWhenBadCredentials() {
        var loginRequest = Map.of(
                "login", "nonexistent",
                "password", "wrong"
        );

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturnBadRequestWhenLoginDataInvalid() {
        var loginRequest = Map.of(
                "login", "",
                "password", ""
        );

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldBlockAccount() {
        var loginRequest = Map.of(
                "login", "block",
                "password", "block"
        );

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized();

        var good = Map.of(
                "login", "block",
                "password", "password"
        );
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(good)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldGenerateRefreshToken() throws InterruptedException {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        var response = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult();

        var oldAccessToken = response.getResponseBody().getAccessToken();
        var oldRefreshToken = response.getResponseBody().getRefreshToken();

        RefreshRequest req = new RefreshRequest(oldRefreshToken);

        var response2 = restTestClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.refreshToken").exists();
}
}
