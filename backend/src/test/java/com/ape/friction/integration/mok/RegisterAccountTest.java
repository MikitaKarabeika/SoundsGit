package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.entities.token.VerificationToken;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.api.exceptions.tokenAndLoginCodes.TokenNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;
import   com.ape.friction.api.mok.repositories.VerificationTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class RegisterAccountTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager mokTransactionManager;

    private final MailpitClient mailpitClient = mailpit.getClient();

    @BeforeEach
    void clearMailbox() {
        mailpitClient.deleteAllMessages();
    }

    private Map<String, Object> validRequest(String email, String login) {
        return Map.of(
                "name", "Jan",
                "surname", "Kowalski",
                "email", email,
                "login", login,
                "password", "Password1!"
        );
    }

    private String registerAndGetVerificationTokenFromEmail(String email, String login) {
        var request = validRequest(email, login);

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();

        assertThat(messages).isNotEmpty();
        assertThat(messages.getFirst().to().getFirst().address()).isEqualTo(email);

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);
        assertThat(emailText).contains("http://localhost:3000/auth/verify?token=");

        System.out.println("=== VERIFICATION EMAIL TEXT ===");
        System.out.println(emailText);
        System.out.println("=== END VERIFICATION EMAIL ===");

        return extractTokenFromEmailText(emailText);
    }

    private VerificationToken findTokenByLogin(String login) {
        return new TransactionTemplate(mokTransactionManager)
                .execute(status -> verificationTokenRepository
                        .findByAccount_Login(login)
                        .orElseThrow());
    }

    private Account findAccountByLogin(String login) {
        return new TransactionTemplate(mokTransactionManager)
                .execute(status -> accountRepository
                        .findByLogin(login)
                        .orElseThrow());
    }

    private boolean tokenExists(String tokenValue) {
        return Boolean.TRUE.equals(new TransactionTemplate(mokTransactionManager)
                .execute(status -> verificationTokenRepository
                        .findByToken(tokenValue)
                        .isPresent()));
    }

    private void expireToken(String login) {
        new TransactionTemplate(mokTransactionManager)
                .executeWithoutResult(status -> {
                    VerificationToken token = verificationTokenRepository
                            .findByAccount_Login(login)
                            .orElseThrow();

                    token.setExpiresDate(Instant.now().minus(1, ChronoUnit.HOURS));
                    verificationTokenRepository.saveAndFlush(token);
                });
    }

    @Test
    void shouldRegisterNewAccountAndSendVerificationEmail() {
        String email = "jan1@example.com";
        String login = "janek1";

        var request = validRequest(email, login);

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.message").exists();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();

        assertThat(messages).isNotEmpty();
        assertThat(messages.getFirst().to().getFirst().address()).isEqualTo(email);

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);

        assertThat(emailText).contains("token=");
        assertThat(extractTokenFromEmailText(emailText)).isNotBlank();
    }

    @Test
    void shouldCreateAccountAsNotVerifiedAfterRegistration() {
        var request = validRequest("notverified@example.com", "notverifieduser");

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        Account account = findAccountByLogin("notverifieduser");

        assertFalse(account.isVerified());
        assertTrue(account.isActive());
    }

    @Test
    void shouldCreateVerificationTokenAfterRegistration() {
        var request = validRequest("token@example.com", "tokenuser");

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        VerificationToken token = findTokenByLogin("tokenuser");

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertThat(token.getToken()).isNotBlank();
    }

    @Test
    void shouldCreateVerificationTokenWithExpiryDateInFuture() {
        var request = validRequest("expiry@example.com", "expiryuser");

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        VerificationToken token = findTokenByLogin("expiryuser");

        assertNotNull(token.getToken());
        assertNotNull(token.getExpiresDate());
        assertTrue(token.getExpiresDate().isAfter(Instant.now()));
        assertTrue(token.getExpiresDate().isBefore(Instant.now().plus(25, ChronoUnit.HOURS)));
    }

    @Test
    void shouldVerifyAccountWhenTokenFromEmailIsValid() {
        String token = registerAndGetVerificationTokenFromEmail(
                "verify-ok@example.com",
                "verifyokuser"
        );

        restTestClient.get()
                .uri("/auth/verify?token=" + token)
                .exchange()
                .expectStatus().isOk();

        Account account = findAccountByLogin("verifyokuser");

        assertTrue(account.isVerified());
    }

    @Test
    void shouldDeleteVerificationTokenAfterSuccessfulVerification() {
        String token = registerAndGetVerificationTokenFromEmail(
                "delete-token@example.com",
                "deletetokenuser"
        );

        restTestClient.get()
                .uri("/auth/verify?token=" + token)
                .exchange()
                .expectStatus().isOk();

        assertFalse(tokenExists(token));
    }

    @Test
    void shouldNotAllowLoginBeforeVerification() {
        var request = validRequest("before-login@example.com", "beforeloginuser");

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", "beforeloginuser",
                        "password", "Password1!"
                ))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldAllowLoginAfterVerificationWithTokenFromEmail() {
        String token = registerAndGetVerificationTokenFromEmail(
                "after-login@example.com",
                "afterloginuser"
        );

        restTestClient.get()
                .uri("/auth/verify?token=" + token)
                .exchange()
                .expectStatus().isOk();

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "login", "afterloginuser",
                        "password", "Password1!"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.refreshToken").exists();
    }

    @Test
    void shouldFailVerificationWhenTokenIsExpired() {
        String email = "expired@example.com";
        String login = "expireduser";

        String token = registerAndGetVerificationTokenFromEmail(email, login);

        expireToken(login);

        restTestClient.get()
                .uri("/auth/verify?token=" + token)
                .exchange()
                .expectStatus().isNotFound();
        System.out.println(token);
    }

    @Test
    void shouldReturn404WhenVerifyTokenDoesNotExist() {
        restTestClient.get()
                .uri("/auth/verify?token=completely-fake-token")
                .exchange()
                .expectStatus().isNotFound();
        //System.out.println(token);
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
