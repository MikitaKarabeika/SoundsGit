package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;


public class DeactivateAccountTest extends MOKIntegrationTestBase {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ShouldReturnDeactivatedAccount() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String token = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        restTestClient.patch()
                .uri("/accounts/b1e44132-a345-44db-9870-a45a18d0665f/deactivate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        restTestClient.patch()
                .uri("/accounts/b1e44132-a345-44db-9870-a45a18d0665f/activate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();


    }

    @Test
    void ShouldReturnErrorNotFound() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String token = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        restTestClient.patch()
                .uri("/accounts/b1e44132-1111-1111-1111-a45a18d0665f/deactivate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();


    }

    @Test
    void ShouldFailToBlockThemselves() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String token = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        restTestClient.patch()
                .uri("/accounts/722e31e2-7e18-493b-a41a-cf64806ea144/deactivate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden();


    }
}
