package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.entities.token.AuthenticationToken;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;
import   com.ape.friction.api.mok.repositories.Authentication2FARepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;


public class TwoFANegativeTest extends MOKIntegrationTestBase {

    private static final String PASSWORD = "Password1!";

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private Authentication2FARepository authentication2FARepository;

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager mokTransactionManager;

    private final MailpitClient mailpitClient = mailpit.getClient();

    @BeforeEach
    void clearMailbox() {
        mailpitClient.deleteAllMessages();
    }

    @Test
    void shouldReturn404WhenAuthSessionIdBelongsToAnotherUser() {
        prepareVerifiedUser("user_a_cross@example.com", "user_a_cross", true);
        prepareVerifiedUser("user_b_cross@example.com", "user_b_cross", true);

        UUID sessionA = loginAndGetAuthSessionId("user_a_cross");
        String tokenA  = getTokenFromEmail();

        mailpitClient.deleteAllMessages();

        UUID sessionB = loginAndGetAuthSessionId("user_b_cross");

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", sessionB,
                        "token", tokenA
                ))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn400WhenTokenIsInvalid() {
        String login = "twofa_invalid_token";
        String email = "twofa_invalid_token@example.com";

        prepareVerifiedUser(email, login, true);

        UUID validSessionId = loginAndGetAuthSessionId(login);

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", validSessionId,
                        "token", "00000000"
                ))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn404WhenAuthSessionIdAndTokenAreInvalid() {
        String login = "twofa_invalid_both";
        String email = "twofa_invalid_both@example.com";
        prepareVerifiedUser(email, login, true);
        loginAndGetAuthSessionId(login);

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", UUID.randomUUID(),
                        "token", "00000000"
                ))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldNotLoginWithWrongToken() {
        String login = "twofa_wrong_token";
        String email = "twofa_wrong_token@example.com";
        prepareVerifiedUser(email, login, true);

        UUID authSessionId = loginAndGetAuthSessionId(login);

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", authSessionId,
                        "token", "00000000"
                ))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(findAuthenticationTokenBySessionId(authSessionId)).isPresent();
    }

    @Test
    void shouldNotLoginWithExpiredToken() {
        String login = "twofa_expired_token";
        String email = "twofa_expired_token@example.com";
        prepareVerifiedUser(email, login, true);

        UUID authSessionId = loginAndGetAuthSessionId(login);
        String validTokenFromEmail = getTokenFromEmail();

        expire2FAToken(authSessionId);

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", authSessionId,
                        "token", validTokenFromEmail
                ))
                .exchange()
                .expectStatus().isNotFound();


    }

    @Test
    void shouldReturn404OnSecondRequestAfterTokenExpired() {
        String login = "twofa_expired_retry";
        String email = "twofa_expired_retry@example.com";
        prepareVerifiedUser(email, login, true);

        UUID authSessionId = loginAndGetAuthSessionId(login);
        String validToken = getTokenFromEmail();

        expire2FAToken(authSessionId);

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "token", validToken))
                .exchange()
                .expectStatus().isNotFound();

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "token", validToken))
                .exchange()
                .expectStatus().isNotFound();
    }


    private RestTestClient.ResponseSpec postTwoFA(UUID authSessionId, String token) {
        return restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", authSessionId,
                        "token", token
                ))
                .exchange();
    }

    private void prepareVerifiedUser(String email, String login, boolean twoFaEnabled) {
        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Jan",
                        "surname", "Kowalski",
                        "email", email,
                        "login", login,
                        "password", PASSWORD
                ))
                .exchange()
                .expectStatus().isCreated();

        new TransactionTemplate(mokTransactionManager)
                .executeWithoutResult(status -> {
                    Account account = accountRepository.findByLogin(login).orElseThrow();

                    account.setVerified(true);
                    account.setActive(true);
                    account.setTwoFaEnabled(twoFaEnabled);

                    accountRepository.saveAndFlush(account);
                });

        mailpitClient.deleteAllMessages();
    }

    private UUID loginAndGetAuthSessionId(String login) {
        Map<String, Object> response = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", login,
                        "password", PASSWORD
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.get("requires2FA")).isEqualTo(true);
        assertThat(response.get("authSessionId")).isNotNull();

        return UUID.fromString(response.get("authSessionId").toString());
    }

    private String getTokenFromEmail() {
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();

        assertThat(messages).isNotEmpty();

        String emailText = mailpitClient.getMessagePlain(messages.getFirst().id());

        return extractEightDigitToken(emailText);
    }

    private String extractEightDigitToken(String emailText) {
        Matcher matcher = Pattern.compile("\\b\\d{8}\\b").matcher(emailText);

        if (matcher.find()) {
            return matcher.group();
        }

        throw new IllegalStateException("Nie znaleziono 8-cyfrowego kodu 2FA w treści maila.");
    }

    private Optional<AuthenticationToken> findAuthenticationTokenBySessionId(UUID authSessionId) {
        return new TransactionTemplate(mokTransactionManager)
                .execute(status -> authentication2FARepository.findByAuthSessionId(authSessionId));
    }

    private void expire2FAToken(UUID authSessionId) {
        new TransactionTemplate(mokTransactionManager)
                .executeWithoutResult(status -> {
                    AuthenticationToken token = authentication2FARepository
                            .findByAuthSessionId(authSessionId)
                            .orElseThrow();

                    token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

                    authentication2FARepository.saveAndFlush(token);
                });
    }
}
