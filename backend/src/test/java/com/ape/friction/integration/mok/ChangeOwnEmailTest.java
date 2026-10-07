// MOK.15 Zmiana adresu e-mail własnego konta

package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.entities.token.EmailChangeToken;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.api.exceptions.tokenAndLoginCodes.TokenNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;
import   com.ape.friction.api.mok.repositories.EmailChangeTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class ChangeOwnEmailTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailChangeTokenRepository emailChangeTokenRepository;

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager mokTransactionManager;

    private final MailpitClient mailpitClient = mailpit.getClient();



    @BeforeEach
    void clearMailboxAndTokens() {
        mailpitClient.deleteAllMessages();
    }

    @Test
    void shouldRequestOwnEmailChangeAndSendVerificationEmail() {
        String login = "spiderman";
        String newEmail = "new-spiderman-" + System.currentTimeMillis() + "@example.com";
        String accessToken = loginAndGetAccessToken(login, "Password1!");

        Account account = findAccountByLogin(login);
        String id = account.getId().toString();
        String currentEmail = account.getEmail();

        var getResponse = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");
        long version = Long.parseLong(getResponse.getResponseBody().get("version").toString());

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", currentEmail,
                        "newEmail", newEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").exists();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();

        assertThat(messages).isNotEmpty();
        assertThat(messages.getFirst().to().getFirst().address()).isEqualTo(newEmail);

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);

        assertThat(emailText).contains("token=");
        String token = extractTokenFromEmailText(emailText);
        assertThat(token).isNotBlank();

        var resetEmailRequestGood = Map.of(
                "token", token
        );

        restTestClient.post()
                .uri("/auth/change-email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestGood)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("spiderman")
                .jsonPath("$.email").isEqualTo(newEmail);

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messagesNew = mailpitClient.getAllMessages();

        assertThat(messagesNew).isNotEmpty();
        assertThat(messagesNew.getFirst().to().getFirst().address()).isEqualTo(currentEmail);

        String messageIdNew = messagesNew.getFirst().id();
        String emailTextNew = mailpitClient.getMessagePlain(messageIdNew);

        assertThat(emailTextNew).contains("token=");
        String tokenNew = extractTokenFromEmailText(emailTextNew);
        assertThat(tokenNew).isNotBlank();

        var resetEmailRequestToOld = Map.of(
                "token", tokenNew
        );

        restTestClient.post()
                .uri("/auth/change-email-to-old")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestToOld)
                .exchange()
                .expectStatus().isOk();

       restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("spiderman")
                .jsonPath("$.email").isEqualTo(currentEmail);
    }

    @Test
    void shouldConfirmOwnEmailChangeButNotGoBackTokenExpired() {
        String login = "ironman";
        String newEmail = "new-ironman-" + System.currentTimeMillis() + "@example.com";
        String accessToken = loginAndGetAccessToken(login, "Password1!");

        Account account = findAccountByLogin(login);
        String id = account.getId().toString();
        String currentEmail = account.getEmail();

        var getResponse = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");
        long version = Long.parseLong(getResponse.getResponseBody().get("version").toString());

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", currentEmail,
                        "newEmail", newEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isOk();

        String emailChangeToken = getEmailChangeTokenFromEmail(newEmail);

        restTestClient.post()
                .uri("/auth/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", emailChangeToken))
                .exchange()
                .expectStatus().isOk();

        Account updatedAccount = findAccountByLogin(login);
        assertEquals(newEmail, updatedAccount.getEmail());
        assertFalse(emailChangeTokenExists(emailChangeToken));

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messagesOld = mailpitClient.getAllMessages();
        assertThat(messagesOld).isNotEmpty();

        String messageIdOld = messagesOld.getFirst().id();
        String emailTextOld = mailpitClient.getMessagePlain(messageIdOld);
        System.out.println("=== EMAIL TEXT ===");
        System.out.println(emailTextOld);
        System.out.println("=== END EMAIL ===");

        String token = extractTokenFromEmailText(emailTextOld);
        TransactionTemplate transactionTemplate = new TransactionTemplate(mokTransactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            EmailChangeToken tokenEntity = emailChangeTokenRepository.findByToken(token)
                    .orElseThrow(TokenNotFoundException::new);

            tokenEntity.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));

            emailChangeTokenRepository.saveAndFlush(tokenEntity);
        });

        var resetEmailRequestExpired = Map.of(
                "token", token
        );

        restTestClient.post()
                .uri("/auth/change-email-to-old")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestExpired)
                .exchange()
                .expectStatus().isEqualTo(410);

        restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("ironman")
                .jsonPath("$.email").isEqualTo(newEmail);

        // Czy usunęło ten token expired
        assertFalse(emailChangeTokenExists(token));



    }

    @Test
    void shouldRejectOwnEmailChangeWhenCurrentEmailIsWrong() {
        String login = "batman";
        String wrongCurrentEmail = "this.is.wrong@example.com";
        String newEmail = "new-batman-" + System.currentTimeMillis() + "@example.com";
        String accessToken = loginAndGetAccessToken(login, "Password1!");

        Account account = findAccountByLogin(login);
        String id = account.getId().toString();

        var getResponse = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");
        long version = Long.parseLong(getResponse.getResponseBody().get("version").toString());

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", wrongCurrentEmail,
                        "newEmail", newEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        assertThat(mailpitClient.getAllMessages()).isEmpty();
    }

    @Test
    void shouldRejectWhenUserAlreadyRequestedEmailChange() {
        String login = "thor";
        String newEmail = "new-thor-" + System.currentTimeMillis() + "@example.com";
        String accessToken = loginAndGetAccessToken(login, "Password1!");

        Account account = findAccountByLogin(login);
        String id = account.getId().toString();
        String currentEmail = account.getEmail();

        var getResponse = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");
        long version = Long.parseLong(getResponse.getResponseBody().get("version").toString());

        var requestBody = Map.of(
                "id", id,
                "currentEmail", currentEmail,
                "newEmail", newEmail,
                "version", version
        );

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk();

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldRejectOwnEmailChangeWhenUserIsNotLoggedIn() {
        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", UUID.randomUUID(),
                        "currentEmail", "some@example.com",
                        "newEmail", "new-not-logged@example.com",
                        "version", 0
                ))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(mailpitClient.getAllMessages()).isEmpty();


        var resetEmailRequestWrong = Map.of(
                "token", "fasfasfafa"
        );

        restTestClient.post()
                .uri("/auth/change-email-to-old")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestWrong)
                .exchange()
                .expectStatus().isNotFound();

    UUID id = UUID.randomUUID();
    String tokenWrong =  UUID.randomUUID().toString();
        var resetEmailRequestWrongv2 = Map.of(
                "token", tokenWrong
        );

        restTestClient.post()
                .uri("/auth/change-email-to-old")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestWrongv2)
                .exchange()
                .expectStatus().isNotFound();

    }
    @Test
    void BadRequestsToEmail() {
        String login = "spiderman";
        String newEmail = "new-spiderman-" + System.currentTimeMillis() + "@example.com";
        String accessToken = loginAndGetAccessToken(login, "Password1!");

        Account account = findAccountByLogin(login);
        String id = account.getId().toString();
        String currentEmail = account.getEmail();

        var getResponse = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");
        long version = Long.parseLong(getResponse.getResponseBody().get("version").toString());

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", "cos tam",
                        "newEmail", newEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", currentEmail,
                        "newEmail", currentEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", currentEmail,
                        "newEmail", newEmail,
                        "version", version-1
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", currentEmail,
                        "newEmail", "",
                        "version", version
                ))
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", "",
                        "newEmail", "",
                        "version", version
                ))
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", "dasdaaaaaaaaaaaaaaaaaa",
                        "currentEmail", currentEmail,
                        "newEmail", newEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", newEmail,
                        "newEmail", newEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isBadRequest();

        // Należy do innego konta nowy email
        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "currentEmail", currentEmail,
                        "newEmail", "tomasz.je@example.com",
                        "version", version
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        // Należy do innego konta email i id
        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", "25c7889a-4a5d-4b1c-9b58-443fe424e6d4",
                        "currentEmail", "tomasz.je@example.com",
                        "newEmail", newEmail,
                        "version", version
                ))
                .exchange()
                .expectStatus().isForbidden();




    }


    private String loginAndGetAccessToken(String login, String password) {
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", password
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();
    }

    private String getEmailChangeTokenFromEmail(String expectedRecipient) {
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();

        assertThat(messages).isNotEmpty();
        assertThat(messages.getFirst().to().getFirst().address()).isEqualTo(expectedRecipient);

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);

        return extractTokenFromEmailText(emailText);
    }

    private Account findAccountByLogin(String login) {
        return new TransactionTemplate(mokTransactionManager)
                .execute(status -> accountRepository
                        .findByLogin(login)
                        .orElseThrow());
    }

    private boolean emailChangeTokenExists(String tokenValue) {
        return Boolean.TRUE.equals(new TransactionTemplate(mokTransactionManager)
                .execute(status -> emailChangeTokenRepository
                        .findByToken(tokenValue)
                        .isPresent()));
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
