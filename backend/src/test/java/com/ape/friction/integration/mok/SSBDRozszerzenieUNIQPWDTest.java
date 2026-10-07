package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;



public class SSBDRozszerzenieUNIQPWDTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;


    @Test
    void shouldNotLetChangeOwnPasswordToOneUsedInPast() {
        String login = "clientchangepassword";
        String oldPassword = "password";
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
                        "newPassword", newPassword
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

        restTestClient.patch()
                .uri("/accounts/me/change-password")
                .header("Authorization", "Bearer " + tokenAfterChange)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "oldPassword", newPassword,
                        "newPassword", oldPassword
                ))
                .exchange()
                .expectStatus().isBadRequest();

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
                .uri("/accounts/b1e55132-a345-44db-9870-a45a18d06231/change-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "newPassword", oldPassword
                ))
                .exchange()
                .expectStatus().isBadRequest();
    }

}
