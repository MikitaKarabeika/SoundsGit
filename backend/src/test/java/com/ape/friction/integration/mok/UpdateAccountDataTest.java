package com.ape.friction.integration.mok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.data.EditOwnAccountDataDTO;
import   com.ape.friction.api.dto.data.EditUserAccountDataDataDTO;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class UpdateAccountDataTest extends MOKIntegrationTestBase {
    @Autowired
    private RestTestClient restTestClient;

    @Test
    public void shouldUpdateAccountData() {
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

        var response = restTestClient.get()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EditUserAccountDataDataDTO.class)
                .returnResult();


        EditUserAccountDataDataDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");


        EditUserAccountDataDataDTO updated = EditUserAccountDataDataDTO.builder()
                .name("Norbert")
                .id(UUID.fromString("f7f9a627-42ed-4c19-8707-8d21456a2194"))
                .surname(dto.surname())
                .version(dto.version())
                .build();


        restTestClient.patch()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .body(updated)
                .exchange()
                .expectStatus().isOk();


        restTestClient.get()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("clientupdate")
                .jsonPath("$.id").isEqualTo("f7f9a627-42ed-4c19-8707-8d21456a2194")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')]").exists()
                .jsonPath("$.name").isEqualTo("Norbert")
                .jsonPath("$.surname").isEqualTo("Gorzelanczyk")
                .jsonPath("$.verified").isEqualTo(true)
                .jsonPath("$.email").isEqualTo("ork.jd@example.com");

    }

    @Test
    public void shouldNotUpdateAccountDataWhenHeaderMissing() {
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

        var response = restTestClient.get()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EditOwnAccountDataDTO.class)
                .returnResult();


        EditOwnAccountDataDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");

        EditUserAccountDataDataDTO updated = EditUserAccountDataDataDTO.builder()
                .name("Norbert")
                .id(UUID.fromString("f7f9a627-42ed-4c19-8707-8d21456a2194"))
                .surname(dto.surname())
                .version(dto.version())
                .build();


        restTestClient.patch()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .body(updated)
                .exchange()
                .expectStatus().isBadRequest();

    }

    @Test
    public void shouldNotUpdateWhenIdIsTampered() {

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


        var response = restTestClient.get()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EditOwnAccountDataDTO.class)
                .returnResult();

        EditOwnAccountDataDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");


        EditUserAccountDataDataDTO updated = EditUserAccountDataDataDTO.builder()
                .name("Norbert")
                .id(UUID.fromString("f7f9a627-42ed-4c19-8707-8d21456a2194"))
                .surname(dto.surname())
                .version(dto.version())
                .build();


        String tamperedId = "722e31e2-7e18-493b-a41a-cf64806ea144";

        restTestClient.patch()
                .uri("/accounts/" + tamperedId)
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .body(updated)
                .exchange()
                .expectStatus().isForbidden();

    }

    @Test
    public void shouldRejectUpdateWhenVersionIsTampered() {

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


        var response = restTestClient.get()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EditOwnAccountDataDTO.class)
                .returnResult();

        EditOwnAccountDataDTO dto = response.getResponseBody();

        String signature = response.getResponseHeaders()
                .getFirst("X-Resource-Signature");


        EditUserAccountDataDataDTO updated = EditUserAccountDataDataDTO.builder()
                .name("Norbert")
                .id(UUID.fromString("f7f9a627-42ed-4c19-8707-8d21456a2194"))
                .surname(dto.surname())
                .version(67L)
                .build();



        restTestClient.patch()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .body(updated)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    public void shouldRejectRequestWhenSignaturePayloadIsTampered() throws Exception {

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


        var getResponse = restTestClient.get()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EditOwnAccountDataDTO.class)
                .returnResult();

        String originalSignature = getResponse.getResponseHeaders()
                .getFirst("X-Resource-Signature");

        EditOwnAccountDataDTO dto = getResponse.getResponseBody();

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

        // ToDo daj w body updated zamiast dto jak coś
        EditUserAccountDataDataDTO updated = EditUserAccountDataDataDTO.builder()
                .name("Norbert")
                .surname(dto.surname())
                .version(999L)
                .build();


        restTestClient.patch()
                .uri("/accounts/f7f9a627-42ed-4c19-8707-8d21456a2194")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", tamperedSignature)
                .body(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

    }
}
