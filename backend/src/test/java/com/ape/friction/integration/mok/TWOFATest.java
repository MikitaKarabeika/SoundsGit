package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.response.login.Login2FARequiredResponseDTO;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class TWOFATest extends MOKIntegrationTestBase {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    MailpitClient mailpitClient = mailpit.getClient();

    @BeforeEach
    void clearMailbox() {
        mailpitClient.deleteAllMessages();
    }

    @Test
    void shouldSuccessfullyLoginWith2FA() {
        var loginRequest = Map.of(
                "login", "admin2",
                "password", "password"
        );

        System.out.println("Przed zalogowaniem:");

        Login2FARequiredResponseDTO response = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Login2FARequiredResponseDTO.class)
                .returnResult()
                .getResponseBody();

      System.out.println("Po zalogowaniu:");

        Assertions.assertNotNull(response);
        assertThat(response.isRequires2FA()).isTrue();


        UUID authSessionId = response.getAuthSessionId();
      assertThat(authSessionId).isNotNull();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String latestMessage = messages.getFirst().id();
        String emailBody = mailpitClient.getMessagePlain(latestMessage);
        /*System.out.println("Pelna tresc maila: " + emailBody);*/
        Matcher matcher = Pattern.compile("\\b\\d{8}\\b").matcher(emailBody);
        assertThat(matcher.find()).as("W mailu powinien znajdować się 8-cyfrowy kod").isTrue();
        String secretCode = matcher.group();
        /*System.out.println("Secret code test: " + secretCode);*/

        var twoFaRequest = Map.of(
                "authSessionId", authSessionId,
                "token", secretCode
        );

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(twoFaRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty();
    }
}
