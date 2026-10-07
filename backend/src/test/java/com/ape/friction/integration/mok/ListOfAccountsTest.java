package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;


public class ListOfAccountsTest extends MOKIntegrationTestBase {


    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ShouldShowListOfAccountsAdmin() {
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
                .uri("/accounts?page=0&size=50")
                .header("Authorization", "Bearer " + token)

                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.page").exists()

                .jsonPath("$._links").exists()
                .jsonPath("$._links.self").exists()
                .jsonPath("$._embedded").exists()

                // Muszą być xd
                .jsonPath("$..[?(@.login == 'admin1')]").exists()
                .jsonPath("$..[?(@.login == 'employee1')]").exists()
                .jsonPath("$..[?(@.login == 'client1')]").exists();

    }


    @Test
    void ShouldNotShowListOfAccountsClient() {
        var loginRequest = Map.of(
                "login", "client1",
                "password", "password"
        );

        String token2 = restTestClient.post()
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
                .uri("/accounts")
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isForbidden();


    }

    @Test
    void ShouldNotShowListOfAccountsEmployee() {
        var loginRequest = Map.of(
                "login", "employee1",
                "password", "password"
        );

        String token3 = restTestClient.post()
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
                .uri("/accounts")
                .header("Authorization", "Bearer " + token3)
                .exchange()
                .expectStatus().isForbidden();


    }
    @Test
    void ShouldNotShowListOfAccountsGuest() {



        restTestClient.get()
                .uri("/accounts")
                .exchange()
                .expectStatus().isUnauthorized();


    }
}
