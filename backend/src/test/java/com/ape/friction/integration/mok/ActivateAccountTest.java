// MOK.10 Odblokuj konto użytkownika. Test

package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import  com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;


public class ActivateAccountTest extends MOKIntegrationTestBase {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ShouldReturnActivatedAccount() {
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
                .uri("/accounts/25c7889a-4a5d-4b1c-9b58-443fe471e6d4/activate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                //.expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().isEmpty();
//                .expectBody()
//                .jsonPath("$").isNotEmpty()
//                .jsonPath("$[0].login").exists()
//                .jsonPath("$[0].active == false").exists();


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
                .uri("/accounts/b1e44132-1111-1111-1111-a45a18d0665f/activate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
        //.expectHeader().contentType(MediaType.APPLICATION_JSON)
        //expectBody().isEmpty();
//                .expectBody()
//                .jsonPath("$").isNotEmpty()
//                .jsonPath("$[0].login").exists()
//                .jsonPath("$[0].active == false").exists();


    }
}
