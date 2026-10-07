package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.dto.response.LoginAuditDTO;
import   com.ape.friction.api.dto.response.login.Login2FARequiredResponseDTO;
import   com.ape.friction.api.dto.response.login.LoginSuccessResponseDTO;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.integration.config.TestTransactionManagersConfig;
import   com.ape.friction.api.mok.repositories.AccountRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@Import(TestTransactionManagersConfig.class)
public class LoginAuditTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager mokTransactionManager;

    private final MailpitClient mailpitClient = mailpit.getClient();

    private static final String PASSWORD = "Password1!";

    @BeforeEach
    void clearMailbox() {
        mailpitClient.deleteAllMessages();
    }

    private String standardLogin(String login, String password) {
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult()
                .getResponseBody()
                .getAccessToken();
    }

    private LoginAuditDTO getMyAudit(String accessToken) {
        return restTestClient.get()
                .uri("/account-details/me/login-audit")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginAuditDTO.class)
                .returnResult()
                .getResponseBody();
    }

    private void prepareUser(String login, String email, boolean twoFaEnabled) {
        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Test",
                        "surname", "User",
                        "email", email,
                        "login", login,
                        "password", PASSWORD
                ))
                .exchange()
                .expectStatus().isCreated();

        new TransactionTemplate(mokTransactionManager).executeWithoutResult(status -> {
            Account account = accountRepository.findByLogin(login).orElseThrow();
            account.setVerified(true);
            account.setActive(true);
            account.setTwoFaEnabled(twoFaEnabled);
            accountRepository.saveAndFlush(account);
        });

        mailpitClient.deleteAllMessages();
    }

    private String twoFaLogin(String login) {
        Login2FARequiredResponseDTO step1 = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Login2FARequiredResponseDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(step1).isNotNull();
        assertThat(step1.isRequires2FA()).isTrue();
        UUID authSessionId = step1.getAuthSessionId();

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        String emailBody = mailpitClient.getMessagePlain(mailpitClient.getAllMessages().getFirst().id());
        Matcher matcher = Pattern.compile("\\b\\d{8}\\b").matcher(emailBody);
        assertThat(matcher.find()).as("8-значный код должен быть в письме").isTrue();
        String code = matcher.group();

        mailpitClient.deleteAllMessages();

        return restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "token", code))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult()
                .getResponseBody()
                .getAccessToken();
    }

    private String codeLogin(String email) {
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

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        String emailBody = mailpitClient.getMessagePlain(mailpitClient.getAllMessages().getFirst().id());
        Matcher matcher = Pattern.compile("\\b[A-Za-z0-9]{10}\\b").matcher(emailBody);
        assertThat(matcher.find()).as("10-значный код должен быть в письме").isTrue();
        String code = matcher.group();

        mailpitClient.deleteAllMessages();

        return restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "code", code))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult()
                .getResponseBody()
                .getAccessToken();
    }

    @Test
    void standardLogin_shouldRecordSuccessfulAudit() {
        String token = standardLogin("employee2", "password");
        LoginAuditDTO audit = getMyAudit(token);

        System.out.println("[standardLogin_shouldRecordSuccessfulAudit] lastLoginAt: " + audit.getLastLoginAt());
        System.out.println("[standardLogin_shouldRecordSuccessfulAudit] lastSuccessfulAuthIp: " + audit.getLastSuccessfulAuthIp());
        System.out.println("[standardLogin_shouldRecordSuccessfulAudit] lastFailedAuthTime: " + audit.getLastFailedAuthTime());
        System.out.println("[standardLogin_shouldRecordSuccessfulAudit] lastFailedAuthIp: " + audit.getLastFailedAuthIp());

        assertThat(audit.getLastLoginAt()).isNotNull();
        assertThat(audit.getLastSuccessfulAuthIp()).isNotNull();
        assertThat(audit.getLastFailedAuthTime()).isNull();
        assertThat(audit.getLastFailedAuthIp()).isNull();
    }

    @Test
    void standardLogin_failedAttempt_shouldRecordFailedAudit() {
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", "client3", "password", "WrongPass1!"))
                .exchange()
                .expectStatus().isUnauthorized();

        String token = standardLogin("client3", "password");
        LoginAuditDTO audit = getMyAudit(token);

        System.out.println("[standardLogin_failedAttempt] lastLoginAt: " + audit.getLastLoginAt());
        System.out.println("[standardLogin_failedAttempt] lastSuccessfulAuthIp: " + audit.getLastSuccessfulAuthIp());
        System.out.println("[standardLogin_failedAttempt] lastFailedAuthTime: " + audit.getLastFailedAuthTime());
        System.out.println("[standardLogin_failedAttempt] lastFailedAuthIp: " + audit.getLastFailedAuthIp());

        assertThat(audit.getLastFailedAuthTime()).isNotNull();
        assertThat(audit.getLastFailedAuthIp()).isNotNull();
        assertThat(audit.getLastLoginAt()).isNotNull();
        assertThat(audit.getLastSuccessfulAuthIp()).isNotNull();
    }

    @Test
    void twoFaLogin_shouldRecordSuccessfulAudit() {
        String login = "audit_2fa_ok";
        String email = "audit_2fa_ok@example.com";
        prepareUser(login, email, true);

        String token = twoFaLogin(login);
        LoginAuditDTO audit = getMyAudit(token);

        System.out.println("[twoFaLogin_shouldRecordSuccessfulAudit] lastLoginAt: " + audit.getLastLoginAt());
        System.out.println("[twoFaLogin_shouldRecordSuccessfulAudit] lastSuccessfulAuthIp: " + audit.getLastSuccessfulAuthIp());
        System.out.println("[twoFaLogin_shouldRecordSuccessfulAudit] lastFailedAuthTime: " + audit.getLastFailedAuthTime());
        System.out.println("[twoFaLogin_shouldRecordSuccessfulAudit] lastFailedAuthIp: " + audit.getLastFailedAuthIp());

        assertThat(audit.getLastLoginAt()).isNotNull();
        assertThat(audit.getLastSuccessfulAuthIp()).isNotNull();
        assertThat(audit.getLastFailedAuthTime()).isNull();
        assertThat(audit.getLastFailedAuthIp()).isNull();
    }

    @Test
    void twoFaLogin_wrongCodeThenSuccess_shouldRecordBothAuditFields() {
        String login = "audit_2fa_fail";
        String email = "audit_2fa_fail@example.com";
        prepareUser(login, email, true);

        Login2FARequiredResponseDTO step1 = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Login2FARequiredResponseDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(step1).isNotNull();
        UUID authSessionId = step1.getAuthSessionId();
        System.out.println("[twoFaLogin_wrongCodeThenSuccess] authSessionId: " + authSessionId);

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        String emailBody = mailpitClient.getMessagePlain(mailpitClient.getAllMessages().getFirst().id());
        Matcher matcher = Pattern.compile("\\b\\d{8}\\b").matcher(emailBody);
        assertThat(matcher.find()).isTrue();
        String correctCode = matcher.group();
        System.out.println("[twoFaLogin_wrongCodeThenSuccess] correctCode from email: " + correctCode);
        mailpitClient.deleteAllMessages();

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "token", "00000000"))
                .exchange()
                .expectStatus().isBadRequest();

        System.out.println("[twoFaLogin_wrongCodeThenSuccess] wrong code rejected — sending correct code");

        String token = restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "token", correctCode))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult()
                .getResponseBody()
                .getAccessToken();

        LoginAuditDTO audit = getMyAudit(token);

        System.out.println("[twoFaLogin_wrongCodeThenSuccess] lastLoginAt: " + audit.getLastLoginAt());
        System.out.println("[twoFaLogin_wrongCodeThenSuccess] lastSuccessfulAuthIp: " + audit.getLastSuccessfulAuthIp());
        System.out.println("[twoFaLogin_wrongCodeThenSuccess] lastFailedAuthTime: " + audit.getLastFailedAuthTime());
        System.out.println("[twoFaLogin_wrongCodeThenSuccess] lastFailedAuthIp: " + audit.getLastFailedAuthIp());

        assertThat(audit.getLastLoginAt()).isNotNull();
        assertThat(audit.getLastSuccessfulAuthIp()).isNotNull();
        assertThat(audit.getLastFailedAuthTime()).isNotNull();
        assertThat(audit.getLastFailedAuthIp()).isNotNull();
    }

    @Test
    void codeLogin_shouldRecordSuccessfulAudit() {
        String login = "audit_code_ok";
        String email = "audit_code_ok@example.com";
        prepareUser(login, email, false);

        String token = codeLogin(email);
        LoginAuditDTO audit = getMyAudit(token);

        System.out.println("[codeLogin_shouldRecordSuccessfulAudit] lastLoginAt: " + audit.getLastLoginAt());
        System.out.println("[codeLogin_shouldRecordSuccessfulAudit] lastSuccessfulAuthIp: " + audit.getLastSuccessfulAuthIp());
        System.out.println("[codeLogin_shouldRecordSuccessfulAudit] lastFailedAuthTime: " + audit.getLastFailedAuthTime());
        System.out.println("[codeLogin_shouldRecordSuccessfulAudit] lastFailedAuthIp: " + audit.getLastFailedAuthIp());

        assertThat(audit.getLastLoginAt()).isNotNull();
        assertThat(audit.getLastSuccessfulAuthIp()).isNotNull();
        assertThat(audit.getLastFailedAuthTime()).isNull();
        assertThat(audit.getLastFailedAuthIp()).isNull();
    }

    @Test
    void codeLogin_wrongCodeThenSuccess_shouldRecordBothAuditFields() {
        String login = "audit_code_fail";
        String email = "audit_code_fail@example.com";
        prepareUser(login, email, false);

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

        System.out.println("[codeLogin_wrongCodeThenSuccess] authSessionId: " + authSessionId);

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        String emailBody = mailpitClient.getMessagePlain(mailpitClient.getAllMessages().getFirst().id());
        Matcher matcher = Pattern.compile("\\b[A-Za-z0-9]{10}\\b").matcher(emailBody);
        assertThat(matcher.find()).isTrue();
        String correctCode = matcher.group();
        System.out.println("[codeLogin_wrongCodeThenSuccess] correctCode from email: " + correctCode);
        mailpitClient.deleteAllMessages();

        restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "code", "Abc123XyZ9"))
                .exchange()
                .expectStatus().isUnauthorized();

        System.out.println("[codeLogin_wrongCodeThenSuccess] wrong code rejected — sending correct code");

        String token = restTestClient.post()
                .uri("/auth/login-with-e-mail/codelogin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authSessionId", authSessionId, "code", correctCode))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult()
                .getResponseBody()
                .getAccessToken();

        LoginAuditDTO audit = getMyAudit(token);

        System.out.println("[codeLogin_wrongCodeThenSuccess] lastLoginAt: " + audit.getLastLoginAt());
        System.out.println("[codeLogin_wrongCodeThenSuccess] lastSuccessfulAuthIp: " + audit.getLastSuccessfulAuthIp());
        System.out.println("[codeLogin_wrongCodeThenSuccess] lastFailedAuthTime: " + audit.getLastFailedAuthTime());
        System.out.println("[codeLogin_wrongCodeThenSuccess] lastFailedAuthIp: " + audit.getLastFailedAuthIp());

        assertThat(audit.getLastLoginAt()).isNotNull();
        assertThat(audit.getLastSuccessfulAuthIp()).isNotNull();
        assertThat(audit.getLastFailedAuthTime()).isNotNull();
        assertThat(audit.getLastFailedAuthIp()).isNotNull();
    }

    @Test
    void admin_canReadAuditForUserWhoLoggedInWith2FA() {
        String login = "audit_2fa_admin";
        String email = "audit_2fa_admin@example.com";
        prepareUser(login, email, true);

        twoFaLogin(login);

        UUID userId = new TransactionTemplate(mokTransactionManager).execute(status ->
                accountRepository.findByLogin(login).orElseThrow().getId()
        );
        System.out.println("[admin_canReadAuditForUserWhoLoggedInWith2FA] targetUserId: " + userId);

        String adminToken = standardLogin("admin1", "password");

        LoginAuditDTO auditFromAdmin = restTestClient.get()
                .uri("/account-details/{id}/login-audit", userId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginAuditDTO.class)
                .returnResult()
                .getResponseBody();

        System.out.println("[admin_canReadAuditForUserWhoLoggedInWith2FA] lastLoginAt: " + auditFromAdmin.getLastLoginAt());
        System.out.println("[admin_canReadAuditForUserWhoLoggedInWith2FA] lastSuccessfulAuthIp: " + auditFromAdmin.getLastSuccessfulAuthIp());

        assertThat(auditFromAdmin.getLastLoginAt()).isNotNull();
        assertThat(auditFromAdmin.getLastSuccessfulAuthIp()).isNotNull();
    }

    @Test
    void admin_canReadAuditForUserWhoLoggedInWithEmailCode() {
        String login = "audit_code_admin";
        String email = "audit_code_admin@example.com";
        prepareUser(login, email, false);

        codeLogin(email);

        UUID userId = new TransactionTemplate(mokTransactionManager).execute(status ->
                accountRepository.findByLogin(login).orElseThrow().getId()
        );
        System.out.println("[admin_canReadAuditForUserWhoLoggedInWithEmailCode] targetUserId: " + userId);

        String adminToken = standardLogin("admin1", "password");

        LoginAuditDTO auditFromAdmin = restTestClient.get()
                .uri("/account-details/{id}/login-audit", userId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginAuditDTO.class)
                .returnResult()
                .getResponseBody();

        System.out.println("[admin_canReadAuditForUserWhoLoggedInWithEmailCode] lastLoginAt: " + auditFromAdmin.getLastLoginAt());
        System.out.println("[admin_canReadAuditForUserWhoLoggedInWithEmailCode] lastSuccessfulAuthIp: " + auditFromAdmin.getLastSuccessfulAuthIp());

        assertThat(auditFromAdmin.getLastLoginAt()).isNotNull();
        assertThat(auditFromAdmin.getLastSuccessfulAuthIp()).isNotNull();
    }

    @Test
    void admin_auditReflectsFailedStandardLoginAttempt() {
        String login = "audit_fail_admin_view";
        String email = "audit_fail_admin_view@example.com";
        prepareUser(login, email, false);

        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", "BadPass1!"))
                .exchange()
                .expectStatus().isUnauthorized();

        System.out.println("[admin_auditReflectsFailedStandardLoginAttempt] failed login sent");

        standardLogin(login, PASSWORD);

        UUID userId = new TransactionTemplate(mokTransactionManager).execute(status ->
                accountRepository.findByLogin(login).orElseThrow().getId()
        );
        System.out.println("[admin_auditReflectsFailedStandardLoginAttempt] targetUserId: " + userId);

        String adminToken = standardLogin("admin1", "password");

        LoginAuditDTO auditFromAdmin = restTestClient.get()
                .uri("/account-details/{id}/login-audit", userId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginAuditDTO.class)
                .returnResult()
                .getResponseBody();

        System.out.println("[admin_auditReflectsFailedStandardLoginAttempt] lastLoginAt: " + auditFromAdmin.getLastLoginAt());
        System.out.println("[admin_auditReflectsFailedStandardLoginAttempt] lastSuccessfulAuthIp: " + auditFromAdmin.getLastSuccessfulAuthIp());
        System.out.println("[admin_auditReflectsFailedStandardLoginAttempt] lastFailedAuthTime: " + auditFromAdmin.getLastFailedAuthTime());
        System.out.println("[admin_auditReflectsFailedStandardLoginAttempt] lastFailedAuthIp: " + auditFromAdmin.getLastFailedAuthIp());

        assertThat(auditFromAdmin.getLastFailedAuthTime()).isNotNull();
        assertThat(auditFromAdmin.getLastFailedAuthIp()).isNotNull();
        assertThat(auditFromAdmin.getLastLoginAt()).isNotNull();
    }

    @Test
    void user_cannotReadAnotherUsersAudit_afterAnyLoginMethod() {
        String login = "audit_priv_check";
        String email = "audit_priv_check@example.com";
        prepareUser(login, email, false);

        codeLogin(email);

        UUID userId = new TransactionTemplate(mokTransactionManager).execute(status ->
                accountRepository.findByLogin(login).orElseThrow().getId()
        );
        System.out.println("[user_cannotReadAnotherUsersAudit] targetUserId: " + userId);

        String otherToken = standardLogin("client1", "password");
        System.out.println("[user_cannotReadAnotherUsersAudit] logged in as client1, attempting to read audit of " + login);

        restTestClient.get()
                .uri("/account-details/{id}/login-audit", userId)
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .expectStatus().isForbidden();

        System.out.println("[user_cannotReadAnotherUsersAudit] correctly got 403 Forbidden");
    }
}
