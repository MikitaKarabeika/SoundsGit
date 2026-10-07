package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.common.Language;
import   com.ape.friction.api.dto.data.PreferencesDataDTO;
import   com.ape.friction.api.entities.users.Theme;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;

public class ChangePreferencesTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    private String loginAndGetToken(String login, String password) {
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();
    }

    @Test
    public void shouldChangePreferences() {

        String token = loginAndGetToken( "spiderman", "Password1!");


        var response = restTestClient.get()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PreferencesDataDTO.class)
                .returnResult();

        PreferencesDataDTO req = PreferencesDataDTO.builder()
                .theme(Theme.DARK)
                .language(Language.PL)
                .build();

        System.out.println("testowy test "  + response.getResponseBody());

        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .body(req)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("PL")
                .jsonPath("$.theme").isEqualTo("DARK");

    }


    @Test
    public void shouldFailWhenNoToken() {
        PreferencesDataDTO req = PreferencesDataDTO.builder()
                .theme(Theme.DARK)
                .language(Language.PL)
                .build();

        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .exchange()
                .expectStatus().isUnauthorized(); // 401
    }

    @Test
    public void shouldFailWhenInvalidToken() {
        PreferencesDataDTO req = PreferencesDataDTO.builder()
                .theme(Theme.DARK)
                .language(Language.PL)
                .build();

        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer invalid.token.value")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void shouldFailWithInvalidLanguage() {
        String token = loginAndGetToken("spiderman", "Password1!");

        String invalidBody = "{\"language\": \"INVALID\", \"theme\": \"DARK\"}";
        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidBody)
                .exchange()
                .expectStatus().isBadRequest(); // 400
    }

    @Test
    public void shouldFailWithInvalidTheme() {
        String token = loginAndGetToken("spiderman", "Password1!");

        String invalidBody = "{\"language\": \"PL\", \"theme\": \"INVALID\"}";
        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void shouldFailWithEmptyBody() {
        String token = loginAndGetToken("spiderman", "Password1!");

        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void shouldFailWithMalformedJson() {
        String token = loginAndGetToken("spiderman", "Password1!");

        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{ invalid json }")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void shouldFailWhenBothFieldsAreNull() {
        String token = loginAndGetToken("spiderman", "Password1!");


        PreferencesDataDTO req = PreferencesDataDTO.builder()
                .theme(Theme.LIGHT)
                .language(null)
                .build();


        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .exchange()
                .expectStatus().isOk();

        PreferencesDataDTO req2 = PreferencesDataDTO.builder()
                .theme(null)
                .language(Language.PL)
                .build();


        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(req2)
                .exchange()
                .expectStatus().isOk();

        PreferencesDataDTO req3 = PreferencesDataDTO.builder()
                .theme(null)
                .language(null)
                .build();


        restTestClient.patch()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(req3)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts/me/preferences")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.language").isEqualTo("PL")
                .jsonPath("$.theme").isEqualTo("LIGHT");

    }
}
