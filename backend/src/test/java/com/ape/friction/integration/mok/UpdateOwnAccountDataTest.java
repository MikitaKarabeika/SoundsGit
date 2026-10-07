package com.ape.friction.integration.mok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import  com.ape.friction.api.dto.response.account.AccountResponseDTO;
import com.ape.friction.integration.config.MOKIntegrationTestBase;


import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class UpdateOwnAccountDataTest extends MOKIntegrationTestBase {
    @Autowired
    private RestTestClient restTestClient;

    @Test
    public void shouldUpdateOwnAccountData()
    {
        var loginRequest = Map.of(
                "login", "clientupdate2",
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

        var response = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponseDTO.class)
                .returnResult();


        AccountResponseDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");

        dto.setName("Tytus");
        dto.setSurname("Bomba");

        System.out.println("testowy test "  + response.getResponseBody());

        restTestClient.patch()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .body(dto)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("clientupdate2")
                .jsonPath("$.id").isEqualTo("e6e5bfab-9445-4574-b63d-d383acc2584e")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')]").exists()
                .jsonPath("$.name").isEqualTo("Tytus")
                .jsonPath("$.surname").isEqualTo("Bomba")
                .jsonPath("$.verified").isEqualTo(true)
                .jsonPath("$.email").isEqualTo("kiedy.bomba@example.com");
    }

    @Test
    public void shouldNotUpdateAccountDataWhenHeaderMissing() {
        var loginRequest = Map.of(
                "login", "clientupdate2",
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

        var response = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponseDTO.class)
                .returnResult();


        AccountResponseDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");

        dto.setName("Tytus");
        dto.setSurname("Bomba");


        restTestClient.patch()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .body(dto)
                .exchange()
                .expectStatus().isBadRequest();

    }

    @Test
    public void shouldNotUpdateWhenIdIsTampered() {

        var loginRequest = Map.of(
                "login", "clientupdate2",
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


        var response = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponseDTO.class)
                .returnResult();

        AccountResponseDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");


        dto.setName("Tytus");
        dto.setSurname("Bomba");


        dto.setId(UUID.fromString("722e31e2-7e18-493b-a41a-cf64806ea144"));

        restTestClient.patch()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .body(dto)
                .exchange()
                .expectStatus().isForbidden();

    }

    @Test
    public void shouldRejectUpdateWhenVersionIsTampered() {

        var loginRequest = Map.of(
                "login", "clientupdate2",
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


        var response = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponseDTO.class)
                .returnResult();

        AccountResponseDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");


        dto.setName("Tytus");
        dto.setSurname("Bomba");

        dto.setVersion(67L);


        restTestClient.patch()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .body(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
    @Test
    public void shouldRejectRequestWhenSignaturePayloadIsTampered() throws  Exception {

        var loginRequest = Map.of(
                "login", "clientupdate2",
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


        var getResponse = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponseDTO.class)
                .returnResult();

        String originalSignature = getResponse.getResponseHeaders()
                .getFirst("X-Resource-Signature");

        AccountResponseDTO dto = getResponse.getResponseBody();

        String[] parts = originalSignature.split("\\.");
        assertEquals(3, parts.length);

        ObjectMapper mapper = new ObjectMapper();


        byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);

        JsonNode root = mapper.readTree(decodedPayload);


        ((ObjectNode) root).put("version", 999L);

        byte[] modifiedPayloadBytes = mapper.writeValueAsBytes(root);

        System.out.println("zmodyfikowany " + modifiedPayloadBytes.toString());

        String modifiedPayloadBase64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(modifiedPayloadBytes);


        String tamperedSignature = parts[0] + "." + modifiedPayloadBase64 + "." + parts[2];


        dto.setVersion(999L);


        restTestClient.patch()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", tamperedSignature)
                .body(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

    }


}
