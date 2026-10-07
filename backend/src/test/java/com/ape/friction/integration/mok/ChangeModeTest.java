package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.response.account.AccountResponseDTO;
import   com.ape.friction.api.entities.users.Theme;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class ChangeModeTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    private String loginAsAdmin() {
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", "admin1", "password", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();
    }

    private Object[] fetchMe(String token) {
        var result = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponseDTO.class)
                .returnResult();

        String signature = result.getResponseHeaders().getFirst("X-Resource-Signature");
        AccountResponseDTO dto = result.getResponseBody();
        return new Object[]{dto, signature};
    }

    private void patchTheme(String token, AccountResponseDTO dto, String signature, Theme theme) {
        dto.setTheme(theme);
        restTestClient.patch()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldChangeToDarkThemeAndVerify() {
        String token = loginAsAdmin();

        Object[] current = fetchMe(token);
        AccountResponseDTO dto = (AccountResponseDTO) current[0];
        String signature = (String) current[1];

        patchTheme(token, dto, signature, Theme.DARK);

        Object[] after = fetchMe(token);
        AccountResponseDTO fresh = (AccountResponseDTO) after[0];
        assertThat(fresh.getTheme()).isEqualTo(Theme.DARK);

        current = fetchMe(token);
        dto = (AccountResponseDTO) current[0];
        signature = (String) current[1];

        patchTheme(token, dto, signature, Theme.LIGHT);
    }

    @Test
    void shouldNotChangeIfThemeIsAbsentInDto() {
        String token = loginAsAdmin();

        Object[] current = fetchMe(token);
        AccountResponseDTO dto = (AccountResponseDTO) current[0];
        System.out.println(dto.getId());
        String signature = (String) current[1];

        patchTheme(token, dto, signature, null);

        Object[] after = fetchMe(token);
        AccountResponseDTO fresh = (AccountResponseDTO) after[0];
        assertThat(fresh.getTheme()).isEqualTo(Theme.LIGHT);
    }

    @Test
    void shouldChangeToDarkThenBackToLightAndVerify() {
        String token = loginAsAdmin();

        Object[] current = fetchMe(token);
        AccountResponseDTO dto = (AccountResponseDTO) current[0];
        String sig1 = (String) current[1];
        patchTheme(token, dto, sig1, Theme.DARK);

        Object[] afterDark = fetchMe(token);
        AccountResponseDTO dtoDark = (AccountResponseDTO) afterDark[0];
        String sig2 = (String) afterDark[1];
        assertThat(dtoDark.getTheme()).isEqualTo(Theme.DARK);

        patchTheme(token, dtoDark, sig2, Theme.LIGHT);

        Object[] afterLight = fetchMe(token);
        AccountResponseDTO fresh = (AccountResponseDTO) afterLight[0];
        assertThat(fresh.getTheme()).isEqualTo(Theme.LIGHT);
    }

    @Test
    void shouldRejectInvalidThemeValue() {
        String token = loginAsAdmin();

        var result = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = result.getResponseHeaders().getFirst("X-Resource-Signature");
        Map<String, Object> dto = result.getResponseBody();
        String id = dto.get("id").toString();

        dto.put("theme", "PINK");

        restTestClient.patch()
                .uri("/accounts/me" )
                .header("Authorization", "Bearer " + token)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldRejectThemeUpdateWithoutSignature() {
        String token = loginAsAdmin();

        Object[] current = fetchMe(token);
        AccountResponseDTO dto = (AccountResponseDTO) current[0];
        dto.setTheme(Theme.DARK);

        restTestClient.patch()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldRejectThemeUpdateWithoutAuthentication() {
        String token = loginAsAdmin();

        Object[] current = fetchMe(token);
        AccountResponseDTO dto = (AccountResponseDTO) current[0];
        String signature = (String) current[1];
        dto.setTheme(Theme.DARK);

        restTestClient.patch()
                .uri("/accounts/me" )
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .exchange()
                .expectStatus().isUnauthorized();
    }


}
