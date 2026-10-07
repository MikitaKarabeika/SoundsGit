// MOK.3 Reset Hasła

package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.exceptions.tokenAndLoginCodes.TokenNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;


public class ResetPasswordTest extends MOKIntegrationTestBase {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    MailpitClient mailpitClient = mailpit.getClient();

    @Test
    void ShouldChangePassword() {
        var forgotPasswordRequest = Map.of(
                "email", "anna.nowak@example.com"
        );

        restTestClient.post()
                .uri("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(forgotPasswordRequest)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);
        assertThat(emailText).contains("http://localhost:3000/auth/reset-password?token=");
        System.out.println("=== EMAIL TEXT ===");
        System.out.println(emailText);
        System.out.println("=== END EMAIL ===");
        String token = extractTokenFromEmailText(emailText);

        var resetPasswordRequest = Map.of(
                "token", token,
                "password", "NewTestPassword123!"
        );

        restTestClient.post()
                .uri("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetPasswordRequest)
                .exchange()
                .expectStatus().isOk();

        var loginRequest = Map.of(
                "login", "client1",
                "password", "NewTestPassword123!"
        );

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void ShouldReturnErrorBadPassword() {
        var forgotPasswordRequest = Map.of(
                "email", "anna.nowak@example.com"
        );

        restTestClient.post()
                .uri("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(forgotPasswordRequest)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);
        String token = extractTokenFromEmailText(emailText);

        var resetPasswordRequest = Map.of(
                "token", token,
                "password", "badpassword"
        );

        restTestClient.post()
                .uri("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetPasswordRequest)
                .exchange()
                .expectStatus().is4xxClientError();

        var loginRequest = Map.of(
                "login", "client1",
                "password", "badpassword"
        );

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    private String extractTokenFromEmailText(String emailText) {
        Pattern pattern = Pattern.compile("token=([a-zA-Z0-9\\-]+)");
        Matcher matcher = pattern.matcher(emailText);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new TokenNotFoundException();
    }
}
