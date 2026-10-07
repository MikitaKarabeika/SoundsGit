package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class S3_PROBDETAILS_Test extends MOKIntegrationTestBase {
    @Autowired
    private RestTestClient restTestClient;


    private static final String PASSWORD = "Password1!";


    @Autowired
    private AccountRepository accountRepository;


    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager mokTransactionManager;

    private final MailpitClient mailpitClient = mailpit.getClient();




    @Test
    void shouldReturn400WhenTokenIsInvalid() {
        String login = "twofa_invalid_token_2";
        String email = "twofa_invalid_token2@example.com";

        prepareVerifiedUser(email, login, true);

        UUID validSessionId = loginAndGetAuthSessionId(login);

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                .body(Map.of(
                        "authSessionId", validSessionId,
                        "token", "00000000"
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)

                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();

    }

    @Test
    void shouldReturn404WhenAuthSessionIdAndTokenAreInvalid() {
        String login = "twofa_invalid_both_2";
        String email = "twofa_invalid_both_2@example.com";
        prepareVerifiedUser(email, login, true);
        loginAndGetAuthSessionId(login);

        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                .body(Map.of(
                        "authSessionId", UUID.randomUUID(),
                        "token", "00000000"
                ))
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)

                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();



    }


    @Test
    void shouldReturn401WhenIsUnauthenticated() {
        restTestClient.get()
                .uri("/accounts/me")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE + ";charset=UTF-8")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.type").isEqualTo("tag:ssbd02,2026:unauthorized")
                .jsonPath("$.title").isEqualTo("Unauthorized")
                .jsonPath("$.detail").isEqualTo("You need to authenticate to access this resource")
                .jsonPath("$.instance").exists();




        //jak dodamy tłumaczenie
//        var response2 = restTestClient.get()
//                .uri("/accounts/me")
//                .header(HttpHeaders.ACCEPT_LANGUAGE, "pl")
//                .exchange()
//                .expectStatus().isUnauthorized()
//                .expectBody(ParameterizedTypeReference.forType(ProblemDetail.class));
//
//        System.out.println(response2.returnResult());


    }

    // 404 nieistniejąćy endpoint (NoResourceFoundException)
    @Test
    void shouldReturn404ProblemDetailWhenEndpointNotFound() {
        String token = getAuthToken("client1");

        restTestClient.get()
                .uri("/this/endpoint/does/not/exist")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.detail").exists()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").value(instance ->
                        assertThat(instance.toString()).contains("/this/endpoint/does/not/exist"));
    }

    @Test
    void shouldReturn405ProblemDetailWhenMethodNotAllowed() {
        restTestClient.delete()
                .uri("/auth/login")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(405)
                .jsonPath("$.title").isEqualTo("Method Not Allowed")
                .jsonPath("$.detail").isEqualTo("HTTP method is not supported for this endpoint")
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturn400ProblemDetailWhenBodyIsMalformedJson() {
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body("{ this is not valid json }")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Malformed Request")
                .jsonPath("$.detail").isEqualTo("The request body is not valid JSON")
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturn400ProblemDetailWhenRequestBodyValidationFails() {
        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Validation Error")
                .jsonPath("$.detail").isEqualTo("One or more fields failed validation")
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturn415ProblemDetailWhenContentTypeNotSupported() {
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body("some text")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(415)
                .jsonPath("$.title").isEqualTo("Unsupported Media Type")
                .jsonPath("$.detail").isEqualTo("The provided media type is not supported")
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void errorResponseShouldAlwaysContainRequiredProblemDetailFields() {
        restTestClient.get()
                .uri("/nonexistent-resource-xyz")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .exchange()
                .expectBody()
                .jsonPath("$.status").exists()
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }


    @Test
    void shouldReturn400ProblemDetailWhenPathVariableHasInvalidType() {
        String token = getAuthToken("client1");

        restTestClient.get()
                .uri("/accounts/not-a-uuid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.detail").isEqualTo("Method argument type mismatch")
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").value(instance ->
                        assertThat(instance.toString()).contains("/accounts/not-a-uuid"));
    }

    @Test
    void shouldReturn403ProblemDetailWhenUserHasNoPermission() {
        String token = getAuthToken("client1");
        restTestClient.get()
                .uri("/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.title").isEqualTo("Forbidden")
                .jsonPath("$.detail").isEqualTo("You do not have permission to access this resource")
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").value(instance ->
                        assertThat(instance.toString()).contains("/accounts"));
    }

    @Test
    void shouldReturn406ProblemDetailWhenAcceptHeaderIsNotSupported() {
        String token = getAuthToken("client1");
        restTestClient.get()
                .uri("/accounts/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .exchange()
                .expectStatus().isEqualTo(406)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(406)
                .jsonPath("$.title").isEqualTo("Not Acceptable")
                .jsonPath("$.detail").isEqualTo("Cannot produce a response in the requested format")
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").value(instance ->
                        assertThat(instance.toString()).contains("/accounts/me"));
    }

    @Test
    void problemTypeShouldUseTagUriScheme() {
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body("{ this is not valid json }")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").value(type ->
                        assertThat(type.toString()).startsWith("tag:ssbd02,2026:"));
    }

    @Test
    void problemDetailStatusShouldMatchHttpStatus() {
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body("{ this is not valid json }")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type ->
                        assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }


    @Test
    void shouldReturnProblemDetailWhenTwoFaRequestHasInvalidAuthSessionIdType() {
        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                .body(Map.of(
                        "authSessionId", "not-a-valid-uuid",
                        "token", "00000000"
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturnProblemDetailWhenTwoFaRequestBodyIsEmpty() {
        restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                .body(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturnProblemDetailWhenLoginCredentialsAreInvalid() {
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body(Map.of(
                        "login", "not_existing_user",
                        "password", "WrongPassword1!"
                ))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturnProblemDetailWhenRegisterRequestContainsInvalidEmail() {
        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body(Map.of(
                        "name", "Jan",
                        "surname", "Kowalski",
                        "email", "not-an-email",
                        "login", "invalid_email_user",
                        "password", PASSWORD
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturnProblemDetailWhenRegisterRequestContainsWeakPassword() {
        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body(Map.of(
                        "name", "Jan",
                        "surname", "Kowalski",
                        "email", "weak_password_user@example.com",
                        "login", "weak_password_user",
                        "password", "weak"
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isNotEmpty()
                .jsonPath("$.detail").isNotEmpty()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturn409ProblemDetailWhenResourceAlreadyExists() {
        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .body(Map.of(
                        "name", "Jan",
                        "surname", "Kowalski",
                        "email", "new_client1_email@example.com",
                        "login", "client1",
                        "password", PASSWORD
                ))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.title").isEqualTo("Conflict")
                .jsonPath("$.detail").exists()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
    }

    @Test
    void shouldReturnLocalizedProblemDetailWhenLanguageIsPolish() {
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "pl")
                .body(Map.of(
                        "login", "wrong-login",
                        "password", "wrong-password"
                ))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.title").exists()
                .jsonPath("$.detail").exists()
                .jsonPath("$.type").value(type -> assertThat(type.toString()).startsWith("tag:ssbd02,2026:"))
                .jsonPath("$.instance").exists();
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

    private String getAuthToken(String login) {
        var loginRequest = Map.of(
                "login", login,
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
                .get("accessToken")
                .toString();
    }

}
