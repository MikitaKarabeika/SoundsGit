package com.ape.friction.integration.mok;


import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;

import java.util.Map;


public class ChangeViewAccessLevelTest extends MOKIntegrationTestBase {


    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    MailpitClient mailpitClient = mailpit.getClient();

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager transactionManager;

    @Test
    void ShouldLetViewAccessLevelAdmin() {
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


        restTestClient.post()
                .uri("/accounts/change-view-access-level")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chosenViewLevelName", "CLIENT"))
                .exchange()
                .expectStatus().isOk();

        restTestClient.post()
                .uri("/accounts/change-view-access-level")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chosenViewLevelName", "ADMIN"))
                .exchange()
                .expectStatus().isOk();

        restTestClient.post()
                .uri("/accounts/change-view-access-level")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chosenViewLevelName", "CLIENT"))
                .exchange()
                .expectStatus().isOk();

    }

    @Test
    void ShouldRejectAccessLevelBadRequest() {
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


        restTestClient.post()
                .uri("/accounts/change-view-access-level")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chosenViewLevelName", "EMPLOYEE"))
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.post()
                .uri("/accounts/change-view-access-level")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chosenViewLevelName", " "))
                .exchange()
                .expectStatus().isBadRequest();


    }

}
