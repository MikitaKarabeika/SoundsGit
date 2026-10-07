package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import  com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.Map;


public class AddUserTest extends MOKIntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    private Map<String, Object> validRequest(String email, String login) {
        return Map.of(
                "name", "Jan",
                "surname", "Kowalski",
                "email", email,
                "login", login,
                "password", "Password1!",
                "accessLevels", java.util.List.of(Map.of("levelName", "CLIENT"))
        );
    }

    @Test
    void ShouldAddUserAsAdmin() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        System.out.println("AddUser");

        // Sprawdzamy czy admin może dodać nowego użytkownika
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest("newuser@example.com", "newuser"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.login").isEqualTo("newuser")
                .jsonPath("$.email").isEqualTo("newuser@example.com")
                .jsonPath("$.name").isEqualTo("Jan")
                .jsonPath("$.surname").isEqualTo("Kowalski")
                .jsonPath("$.verified").isEqualTo(true)
                .jsonPath("$.active").isEqualTo(true);
    }

    @Test
    void ShouldHaveAccessLevelLinkedToAccountAfterFetch() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        // Dodajemy nowego użytkownika i wyciągamy jego id
        Map createResponse = restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest("linkcheck@example.com", "linkcheckuser"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String accountId = createResponse.get("id").toString();

        // Pobieramy konto przez GET /accounts/{id} — access levels muszą być powiązane z kontem
        // (jeśli account_id w access_level byłby null, FK constraint NOT NULL by wywalił INSERT
        // przy zapisie, więc samo zwrócenie konta z access levelem potwierdza powiązanie)
        restTestClient.get()
                .uri("/accounts/{id}", accountId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(accountId)
                .jsonPath("$.login").isEqualTo("linkcheckuser")
                .jsonPath("$.accessLevels").isNotEmpty()
                .jsonPath("$.accessLevels[0].levelName").isEqualTo("CLIENT")
                .jsonPath("$.accessLevels[0].active").isEqualTo(true);
    }

    @Test
    void ShouldAddedUserBeVerified() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        // Dodajemy użytkownika przez admina
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest("verified@example.com", "verifieduser"))
                .exchange()
                .expectStatus().isCreated();

        // Sprawdzamy czy konto jest od razu zweryfikowane
        restTestClient.get()
                .uri("/accounts?page=0&size=50")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$..[?(@.login == 'verifieduser')].verified").isEqualTo(true);
    }

    @Test
    void ShouldFailWhenLoginAlreadyExists() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        var request1 = validRequest("first@example.com", "dupLogin");
        var request2 = validRequest("second@example.com", "dupLogin");

        // pierwsza operacja OK
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request1)
                .exchange()
                .expectStatus().isCreated();

        // druga nie OK - login zajęty
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void ShouldFailWhenEmailAlreadyExists() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        var request1 = validRequest("dupemail@example.com", "loginA");
        var request2 = validRequest("dupemail@example.com", "loginB");

        // pierwsza operacja OK
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request1)
                .exchange()
                .expectStatus().isCreated();

        // druga nie OK - email zajęty
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void ShouldFailValidationWhenInvalidData() {
        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        var invalidRequest = Map.of(
                "name", "",
                "surname", "",
                "email", "not-an-email",
                "login", "",
                "password", "123"
        );

        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }


    private String loginAsAdmin() {
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

    private Map<String, Object> requestWithoutField(String fieldToOmit) {
        Map<String, Object> base = new java.util.HashMap<>();
        base.put("name", "Jan");
        base.put("surname", "Kowalski");
        base.put("email", "neg@example.com");
        base.put("login", "neguser");
        base.put("password", "Password1!");
        base.put("accessLevels", java.util.List.of(Map.of("levelName", "CLIENT")));
        base.remove(fieldToOmit);
        return base;
    }

    private Map<String, Object> requestWithField(String field, Object value) {
        Map<String, Object> base = new java.util.HashMap<>(requestWithoutField(""));
        base.put(field, value);
        return base;
    }

    @Test
    void ShouldFailWhenNoAuthToken() {
        restTestClient.post()
                .uri("/accounts/add-user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest("noauth@example.com", "noauthuser"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void ShouldFailWhenAccessLevelsMissing() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithoutField("accessLevels"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenAccessLevelsEmpty() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("accessLevels", java.util.List.of()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenAccessLevelNameInvalid() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("accessLevels", java.util.List.of(Map.of("levelName", "FOOBAR"))))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenLoginContainsSpecialChars() {
        String adminToken = loginAsAdmin();
        // login ze znakami specjalnymi — backend regex ^[a-zA-Z0-9_]+$ odrzuca @#!- itp.
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("login", "!@#@-----"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------- Walidacja pola: name ----------

    @Test
    void ShouldFailWhenNameBlank() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("name", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenNameTooLong() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("name", "J".repeat(33)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------- Walidacja pola: surname ----------

    @Test
    void ShouldFailWhenSurnameBlank() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("surname", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenSurnameTooLong() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("surname", "K".repeat(33)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------- Walidacja pola: email ----------

    @Test
    void ShouldFailWhenEmailBlank() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("email", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenEmailInvalidFormat() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("email", "not-an-email"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenEmailTooLong() {
        String adminToken = loginAsAdmin();
        // local-part 117 znaków + "@example.com" (12) = 129 > 128
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("email", "a".repeat(117) + "@example.com"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------- Walidacja pola: login ----------

    @Test
    void ShouldFailWhenLoginBlank() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("login", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenLoginTooShort() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("login", "ab"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenLoginTooLong() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("login", "a".repeat(33)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------- Walidacja pola: password ----------

    @Test
    void ShouldFailWhenPasswordBlank() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("password", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenPasswordTooShort() {
        String adminToken = loginAsAdmin();
        // "Pas1!" — spełnia regex złożoności, ale < 8 znaków
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("password", "Pas1!"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenPasswordTooLong() {
        String adminToken = loginAsAdmin();
        // 65 znaków — spełnia regex złożoności, ale > 64
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("password", "Aa1!" + "x".repeat(61)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenPasswordMissingDigit() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("password", "Password!"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenPasswordMissingLowercase() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("password", "PASSWORD1!"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenPasswordMissingUppercase() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("password", "password1!"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenPasswordMissingSpecialChar() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("password", "Password11"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------- Walidacja pola: accessLevels ----------

    @Test
    void ShouldFailWhenAccessLevelNameBlank() {
        String adminToken = loginAsAdmin();
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("accessLevels", java.util.List.of(Map.of("levelName", ""))))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ---------- Walidacja klasowa: @AssertTrue isLoginUniqueAmongFields ----------

    @Test
    void ShouldFailWhenLoginEqualsName() {
        String adminToken = loginAsAdmin();
        // base name = "Jan"; login = "Jan" -> login nie może być równy name
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("login", "Jan"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void ShouldFailWhenLoginEqualsSurname() {
        String adminToken = loginAsAdmin();
        // base surname = "Kowalski"; login = "Kowalski" -> login nie może być równy surname
        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestWithField("login", "Kowalski"))
                .exchange()
                .expectStatus().isBadRequest();
    }

   /* @Test
    void ShouldNotAllowClientToAddUser() {
        var loginRequest = Map.of(
                "login", "client1",
                "password", "password"
        );

        String clientToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest("unauthorized@example.com", "unauthorized1"))
                .exchange()
                .expectStatus().isUnauthorized();
    }*/


}
