package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import io.jsonwebtoken.Claims;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.entities.token.LoginCode;
import   com.ape.friction.api.exceptions.tokenAndLoginCodes.CodeNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.LoginCodeRepository;
import   com.ape.friction.api.mok.services.JwtService;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;


public class SSBDRozszerzenieCodeLoginTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private LoginCodeRepository loginCodeRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager transactionManager;

    MailpitClient mailpitClient = mailpit.getClient();
    private static final String EXISTING_EMAIL = "anna.nowak@example.com";

    record CodeAndSession(String authSessionId, String code) {}

    @BeforeEach
    void cleanLoginCodes() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> loginCodeRepository.deleteAll());
    }

    @Test
    void ShouldSendLoginCodeToExistingActiveVerifiedAccount() {
        int messagesBefore = mailpitClient.getAllMessages().size();

        var request = Map.of(
                "email", EXISTING_EMAIL
        );

        restTestClient.post()
                .uri("/auth/login-with-e-mail")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.authSessionId").exists();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> mailpitClient.getAllMessages().size() > messagesBefore);

        var codes = loginCodeRepository.findAll();
        assertThat(codes).hasSize(1);
        assertThat(codes.getFirst().getCode()).doesNotMatch("^[A-Za-z0-9]{10}$");
        assertThat(codes.getFirst().getCode().length()).isGreaterThan(20);
        assertThat(codes.getFirst().getExpiresDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void ShouldLoginUsingCode() {
        CodeAndSession data = sendCodeAndExtractData(EXISTING_EMAIL);

        String accessToken = restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", data.authSessionId(),
                        "code", data.code()
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();

        restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void ShouldRemoveUsedCodeAfterSuccessfulLogin() {
        CodeAndSession data = sendCodeAndExtractData(EXISTING_EMAIL);

        restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", data.authSessionId(),
                        "code", data.code()
                ))
                .exchange()
                .expectStatus().isOk();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status ->
                assertThat(loginCodeRepository.findByAuthSessionId(UUID.fromString(data.authSessionId()))).isEmpty()
        );
    }

    @Test
    void ShouldNotAllowUsingTheSameCodeTwice() {
        CodeAndSession data = sendCodeAndExtractData(EXISTING_EMAIL);

        var requestBody = Map.of(
                "authSessionId", data.authSessionId(),
                "code", data.code()
        );

        restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk();

        restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void ShouldReturnNotFoundOrUnauthorizedForWrongCode() {
        CodeAndSession data = sendCodeAndExtractData(EXISTING_EMAIL);

        var request = Map.of(
                "authSessionId", data.authSessionId(),
                "code", "Abc123XyZ9"
        );

        restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void ShouldReturnBadRequestWhenCodeHasWrongLengthAndRandomSessionId() {
        var request = Map.of(
                "authSessionId", UUID.randomUUID().toString(),
                "code", "short"
        );

        restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldReturnNotFoundForExpiredCode() {
        CodeAndSession data = sendCodeAndExtractData(EXISTING_EMAIL);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            LoginCode loginCode = loginCodeRepository.findByAuthSessionId(UUID.fromString(data.authSessionId()))
                    .orElseThrow(CodeNotFoundException::new);

            loginCode.setExpiresDate(LocalDateTime.now().minusMinutes(5));
            loginCodeRepository.saveAndFlush(loginCode);
        });

        var request = Map.of(
                "authSessionId", data.authSessionId(),
                "code", data.code()
        );

        restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ShouldNotSendCodeForUnknownEmailButStillReturnOk() {
        int messagesBefore = mailpitClient.getAllMessages().size();

        var request = Map.of(
                "email", "unknown-user@example.com"
        );

        restTestClient.post()
                .uri("/auth/login-with-e-mail")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.authSessionId").exists(); // Nawet fejkowe zapytanie daje ID, zeby nie zdradzać istnienia usera

        assertThat(loginCodeRepository.findAll()).isEmpty();
        assertThat(mailpitClient.getAllMessages().size()).isEqualTo(messagesBefore);
    }

    @Test
    void CodeLoginSessionShouldBeTwiceShorterThanStandardLoginSession() {
        var loginRequest = Map.of(
                "login", "client1",
                "password", "password"
        );

        Map standardLoginResponse = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        CodeAndSession data = sendCodeAndExtractData(EXISTING_EMAIL);

        Map codeLoginResponse = restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "authSessionId", data.authSessionId(),
                        "code", data.code()
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        long standardAccessLifetime = getTokenLifetimeMillis(standardLoginResponse.get("accessToken").toString());
        long codeAccessLifetime = getTokenLifetimeMillis(codeLoginResponse.get("accessToken").toString());

        long standardRefreshLifetime = getTokenLifetimeMillis(standardLoginResponse.get("refreshToken").toString());
        long codeRefreshLifetime = getTokenLifetimeMillis(codeLoginResponse.get("refreshToken").toString());

        assertThat(codeAccessLifetime).isLessThanOrEqualTo(standardAccessLifetime / 2 + 1000);
        assertThat(codeRefreshLifetime).isLessThanOrEqualTo(standardRefreshLifetime / 2 + 1000);
    }

    private long getTokenLifetimeMillis(String token) {
        Claims claims = jwtService.parseClaims(token).orElseThrow();
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        return expiration.getTime() - issuedAt.getTime();
    }

    private CodeAndSession sendCodeAndExtractData(String email) {
        mailpitClient.deleteAllMessages();

        String authSessionId = restTestClient.post()
                .uri("/auth/login-with-e-mail")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("authSessionId")
                .toString();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String emailText = mailpitClient.getMessagePlain(messages.getFirst().id());
        String code = extractCodeFromEmailText(emailText);

        return new CodeAndSession(authSessionId, code);
    }

    private String extractCodeFromEmailText(String emailText) {
        Pattern pattern = Pattern.compile("\\b[A-Za-z0-9]{10}\\b");
        Matcher matcher = pattern.matcher(emailText);

        if (matcher.find()) {
            return matcher.group();
        }

        throw new RuntimeException("Login code not found in email");
    }
}
