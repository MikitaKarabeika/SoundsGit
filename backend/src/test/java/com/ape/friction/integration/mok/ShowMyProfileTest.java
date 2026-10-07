package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;



public class ShowMyProfileTest extends MOKIntegrationTestBase {


    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ShouldShowMyProfileClient() {
        var loginRequest = Map.of(
                "login", "client1",
                "password", "password"
        );

        String clientToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();




        restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + clientToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("client1")
                .jsonPath("$.id").isEqualTo("b1e44132-a345-44db-9870-a45a18d0665f")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')]").exists()
                .jsonPath("$.name").isEqualTo("Anna")
                .jsonPath("$.surname").isEqualTo("Nowak")
                .jsonPath("$.email").isEqualTo("anna.nowak@example.com");



    }

    @Test
    void ShouldShowMyProfileAdmin() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();




        restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("admin1")
                .jsonPath("$.id").isEqualTo("722e31e2-7e18-493b-a41a-cf64806ea144")
                .jsonPath("$.accessLevels[?(@.levelName == 'ADMIN')]").exists()
                .jsonPath("$.name").isEqualTo("Jan")
                .jsonPath("$.surname").isEqualTo("Kowalski")
                .jsonPath("$.email").isEqualTo("jan.kowalski@example.com");



    }

    @Test
    void ShouldShowMyProfileClientAccount() {
        var loginRequest = Map.of(
                "login", "employee1",
                "password", "password"
        );

        String employeeToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();




        restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + employeeToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("employee1")
                .jsonPath("$.id").isEqualTo("25c7889a-4a5d-4b1c-9b58-443fe471e6d4")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')]").exists()
                .jsonPath("$.name").isEqualTo("Piotr")
                .jsonPath("$.surname").isEqualTo("Wiśniewski")
                .jsonPath("$.email").isEqualTo("piotr.wisniewski@example.com");



    }

    @Test
    void ShouldThrowErrorNoAccount() {

        restTestClient.get()
                .uri("/accounts/me")
                .exchange()
                .expectStatus().isUnauthorized();


    }

}
