package com.ape.friction.integration.mok;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;


public class GetOtherAccountTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void shouldGetOtherAccountWithHeader() {
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

        restTestClient.get()
                .uri("/accounts/b1e44132-a345-44db-9870-a45a18d0665f")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("client1")
                .jsonPath("$.id").isEqualTo("b1e44132-a345-44db-9870-a45a18d0665f")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')]").exists()
                .jsonPath("$.name").isEqualTo("Anna")
                .jsonPath("$.surname").isEqualTo("Nowak")
                .jsonPath("$.verified").isEqualTo(true)
                .jsonPath("$.email").isEqualTo("anna.nowak@example.com");

    }

    @Test
    void shouldNotGetOtherAccountWithHeader() {
        Map<String,String> loginRequest = Map.of(
                "login", "client1",
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


        restTestClient.get()
                .uri("/accounts/722e31e2-7e18-493b-a41a-cf64806ea144")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();

    }

 @Test
    void shouldNotGetOtherAccountBecauseItsYouAdmin() {
        Map<String,String> loginRequest = Map.of(
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


        restTestClient.get()
                .uri("/accounts/722e31e2-7e18-493b-a41a-cf64806ea144")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .consumeWith(System.out::println);

    }

    @Test
    void shouldNotGetOtherAccountWithHeader_notFound() {
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

        restTestClient.get()
                .uri("/accounts/11111111-1111-1111-1111-111111111111")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();

    }

    @Test
    void shouldNotGetOtherAccountWithHeader_badRequest() {
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

        restTestClient.get()
                .uri("/accounts/disco-polo-rzadzi")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest();
    }

}
