package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.data.EditTimeZoneDataDTO;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class SSBDRozszerzenieTZONETest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    public void shouldGetAndUpdateMyTimeZoneSuccessfully() {
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
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();


        Map<String, Object> currentMap = getResponse.getResponseBody();
        assertNotNull(currentMap);
        System.out.println("DUPA: " + currentMap);
        String oldTimezone = (String) currentMap.get("timezone");

        System.out.println("old timezone: " + oldTimezone);

        String newTimezone = "Europe/Warsaw";
        EditTimeZoneDataDTO updateRequest = new EditTimeZoneDataDTO(newTimezone);

        var patchResponse = restTestClient.patch()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EditTimeZoneDataDTO.class)
                .returnResult();

        EditTimeZoneDataDTO updatedDto = patchResponse.getResponseBody();
        assertNotNull(updatedDto);
        assertEquals(newTimezone, updatedDto.timezone());
        assertNotEquals(oldTimezone, updatedDto.timezone());

        var getAfter = restTestClient.get()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        assertEquals(newTimezone, (String) getAfter.getResponseBody().get("timezone"));
    }

    @Test
    public void shouldNotUpdateTimeZoneWhenNoToken() {
        // Wyślij PATCH bez nagłówka Authorization
        EditTimeZoneDataDTO updateRequest = new EditTimeZoneDataDTO("Europe/Warsaw");

        restTestClient.patch()
                .uri("/account-details/me/timezone")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .exchange()
                .expectStatus().isUnauthorized(); // 401
    }

    @Test
    public void shouldNotGetTimeZoneWhenNoToken() {
        restTestClient.get()
                .uri("/account-details/me/timezone")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void shouldReturnBadRequestWhenTimezoneInvalid() {
        // 1. Zaloguj się
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


        EditTimeZoneDataDTO invalidRequest = new EditTimeZoneDataDTO("");

        restTestClient.patch()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();

        EditTimeZoneDataDTO invalidRequest2 = new EditTimeZoneDataDTO("67");

        restTestClient.patch()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest2)
                .exchange()
                .expectStatus().isBadRequest();

        EditTimeZoneDataDTO invalidRequest3 = new EditTimeZoneDataDTO("Aura/Farmer");

        restTestClient.patch()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest3)
                .exchange()
                .expectStatus().isBadRequest();

        EditTimeZoneDataDTO invalidRequest4 = new EditTimeZoneDataDTO(" /Warsaw");

        restTestClient.patch()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest4)
                .exchange()
                .expectStatus().isBadRequest();


        EditTimeZoneDataDTO invalidRequest5 = new EditTimeZoneDataDTO("EuRoPe/WaRsAw");

        restTestClient.patch()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest5)
                .exchange()
                .expectStatus().isBadRequest();

        EditTimeZoneDataDTO invalidRequest6 = new EditTimeZoneDataDTO("USA/WaRsAw");

        restTestClient.patch()
                .uri("/account-details/me/timezone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest6)
                .exchange()
                .expectStatus().isBadRequest();
    }


}
