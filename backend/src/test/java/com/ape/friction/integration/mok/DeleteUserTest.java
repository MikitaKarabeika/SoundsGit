package com.ape.friction.integration.mok;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class DeleteUserTest extends MOKIntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    private Map<String, Object> validRequest(String email, String login) {
        return Map.of(
                "name", "Jan",
                "surname", "Kowalski",
                "email", email,
                "login", login,
                "password", "Password1!"
        );
    }

    @Test
    void ShouldReturnDeletedAccount() {
        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest("todelete@example.com", "todelete1"))
                .exchange()
                .expectStatus().isCreated();

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

        byte[] body = restTestClient.get()
                .uri("/accounts?page=0&size=50")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$..[?(@.login == 'todelete1')]").exists()
                .returnResult()
                .getResponseBody();

        List<String> ids = JsonPath.read(new String(body), "$..[?(@.login == 'todelete1')].id");
        String userId = ids.get(0);

        restTestClient.delete()
                .uri("/accounts/{id}/delete", userId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        byte[] body2 = restTestClient.get()
                .uri("/accounts")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.login == 'todelete1')]").doesNotExist()
                .returnResult()
                .getResponseBody();

        restTestClient.delete()
                .uri("/accounts/{id}/delete", userId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();

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

        UUID nonExistentId = UUID.randomUUID();

        restTestClient.delete()
                .uri("/accounts/{id}/delete", nonExistentId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();


    }

    @Test
    void ShouldNotAllowDeleteOwnAccount() {
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

        // Pobieramy własne ID admina przez /accounts/me
        byte[] body = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        String adminId = JsonPath.read(new String(body), "$.id");

        restTestClient.delete()
                .uri("/accounts/{id}/delete", adminId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();


    }


}
