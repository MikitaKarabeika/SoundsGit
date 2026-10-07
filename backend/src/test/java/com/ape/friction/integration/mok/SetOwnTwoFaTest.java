package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// S2_KODYJEDN — testy własnego toggle 2FA przez user'a

public class SetOwnTwoFaTest extends MOKIntegrationTestBase {

    private static final String PASSWORD = "Password1!";

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager mokTransactionManager;

    @Test
    void shouldEnableTwoFa() {
        String login = "twofa_toggle_enable";
        prepareVerifiedUser(login, "twofa_toggle_enable@example.com", false);

        String accessToken = loginAndGetToken(login);

        restTestClient.patch()
                .uri("/accounts/me/two-factor-auth")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", true))
                .exchange()
                .expectStatus().isOk();

        assertThat(currentTwoFaFlag(login)).isTrue();
    }

    @Test
    void shouldDisableTwoFa() {
        String login = "twofa_toggle_disable";
        // Najpierw bez 2FA — żeby login dał JWT accessToken (z 2FA on login zwróciłby tylko authSessionId).
        prepareVerifiedUser(login, "twofa_toggle_disable@example.com", false);
        String accessToken = loginAndGetToken(login);
        // Teraz włącz 2FA w bazie — accessToken JWT jest dalej ważny (niezależny od tej flagi).
        setTwoFaInDb(login, true);

        restTestClient.patch()
                .uri("/accounts/me/two-factor-auth")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", false))
                .exchange()
                .expectStatus().isOk();

        assertThat(currentTwoFaFlag(login)).isFalse();
    }

    @Test
    void shouldRejectWhenNotAuthenticated() {
        restTestClient.patch()
                .uri("/accounts/me/two-factor-auth")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", true))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRequireTwoFaAfterEnabling() {
        String login = "twofa_toggle_flow";
        prepareVerifiedUser(login, "twofa_toggle_flow@example.com", false);

        String accessToken = loginAndGetToken(login);

        restTestClient.patch()
                .uri("/accounts/me/two-factor-auth")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", true))
                .exchange()
                .expectStatus().isOk();

        @SuppressWarnings("unchecked")
        Map<String, Object> nextLogin = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(nextLogin).isNotNull();
        assertThat(nextLogin.get("requires2FA")).isEqualTo(true);
        assertThat(nextLogin.get("authSessionId")).isNotNull();
        assertThat(nextLogin.get("accessToken")).isNull();
    }

    private void prepareVerifiedUser(String login, String email, boolean twoFaEnabled) {
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
    }

    private String loginAndGetToken(String login) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        Object token = body.get("accessToken");
        assertThat(token).isNotNull();
        return token.toString();
    }

    private boolean currentTwoFaFlag(String login) {
        Boolean result = new TransactionTemplate(mokTransactionManager)
                .execute(status -> accountRepository.findByLogin(login)
                        .orElseThrow()
                        .isTwoFaEnabled());
        return Boolean.TRUE.equals(result);
    }

    private void setTwoFaInDb(String login, boolean enabled) {
        new TransactionTemplate(mokTransactionManager)
                .executeWithoutResult(status -> {
                    Account account = accountRepository.findByLogin(login).orElseThrow();
                    account.setTwoFaEnabled(enabled);
                    accountRepository.saveAndFlush(account);
                });
    }
}
