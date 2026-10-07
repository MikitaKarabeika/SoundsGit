// S2_POWEMAIL

package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import nl.altindag.log.LogCaptor;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.exceptions.tokenAndLoginCodes.TokenNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.interceptors.MethodCallLoggingInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;


public class NotifyTest extends MOKIntegrationTestBase {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    MailpitClient mailpitClient = mailpit.getClient();

    private String getAdminToken() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();
    }

    @Test
    void shouldSendEmailNotificationWhenAccountIsDeactivated() {
        String token = getAdminToken();
        mailpitClient.deleteAllMessages();
        restTestClient.patch()
                .uri("/accounts/{id}/deactivate", "3a5f889a-4a5d-4b1c-9b58-443fe471e999")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String emailBody = mailpitClient.getMessagePlain(messages.getLast().id());
        System.out.println("Treść maila deaktywacja: " + emailBody);
        assertThat(emailBody).matches("(?is).*(zablokowane|deactivated).*");
    }

    @Test
    void shouldSendEmailNotificationWhenAccountIsActivated() {
        String token = getAdminToken();

        restTestClient.patch()
                .uri("/accounts/{id}/deactivate", "3a5f889a-4a5d-4b1c-9b58-443fe471e999")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        mailpitClient.deleteAllMessages();

        restTestClient.patch()
                .uri("/accounts/{id}/activate", "3a5f889a-4a5d-4b1c-9b58-443fe471e999")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String emailBody = mailpitClient.getMessagePlain(messages.getLast().id());
        System.out.println("Treść maila aktywacja: " + emailBody);
        assertThat(emailBody).matches("(?is).*(odblokowane|activated).*");
    }

    @Test
    void shouldSendEmailNotificationAfterSuccessfulAccountVerification() {
        var registerRequest = Map.of(
                "name", "Jan",
                "surname", "Kowalski",
                "email", "notify-verify@example.com",
                "login", "notifyverifyuser",
                "password", "Password1!"
        );

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .exchange()
                .expectStatus().isCreated();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String verificationEmailText = mailpitClient.getMessagePlain(messages.getFirst().id());
        String verificationToken = extractTokenFromEmailText(verificationEmailText);

        mailpitClient.deleteAllMessages();

        restTestClient.get()
                .uri("/auth/verify?token=" + verificationToken)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var confirmationMessages = mailpitClient.getAllMessages();
        assertThat(confirmationMessages).isNotEmpty();

        String confirmationEmail = mailpitClient.getMessagePlain(confirmationMessages.getLast().id());
        System.out.println("Treść maila potwierdzenia weryfikacji: " + confirmationEmail);

        assertThat(confirmationMessages.getFirst().to().getFirst().address())
                .isEqualTo("notify-verify@example.com");
        assertThat(confirmationEmail).matches("(?is).*(verified|zweryfikowane).*");
    }

    private String extractTokenFromEmailText(String emailText) {
        Pattern pattern = Pattern.compile("token=([a-zA-Z0-9\\-]+)");
        Matcher matcher = pattern.matcher(emailText);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new TokenNotFoundException();
    }

    @Test
    void shouldNotificateAdmin(){
        mailpitClient.deleteAllMessages();
        getAdminToken();

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var confirmationMessages = mailpitClient.getAllMessages();
        assertThat(confirmationMessages).isNotEmpty();

        String confirmationEmail = mailpitClient.getMessagePlain(confirmationMessages.getFirst().id());
        assertThat(confirmationEmail).matches("(?is).*(admin1).*");

    }


    @Test
    void shouldLogChangingViewAccessLevel() {
        try (LogCaptor logCaptor = LogCaptor.forClass(MethodCallLoggingInterceptor.class)) {
            String token = getAdminToken();

            var requestBody = Map.of("chosenViewLevelName", "ADMIN");

            restTestClient.post()
                    .uri("/accounts/change-view-access-level")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .exchange()
                    .expectStatus().isOk();


            System.out.println(logCaptor.getInfoLogs());

        }


    }
}
