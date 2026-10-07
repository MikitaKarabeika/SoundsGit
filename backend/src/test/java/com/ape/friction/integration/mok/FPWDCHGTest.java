package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import static org.assertj.core.api.Assertions.assertThat;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.request.SetNewPasswordRequestDTO;
import   com.ape.friction.api.dto.response.login.Login2FARequiredResponseDTO;
import   com.ape.friction.api.dto.response.login.LoginRequiresPasswordChange;
import   com.ape.friction.api.dto.response.login.LoginSuccessResponseDTO;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FPWDCHGTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    MailpitClient mailpitClient = mailpit.getClient();


    private String uniqueLogin(String base) {
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String uniqueEmail(String base) {
        return base + "_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private LoginRequiresPasswordChange loginExpectingNewPassword(String login, String password) {
        var loginRequest = Map.of("login", login, "password", password);
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginRequiresPasswordChange.class)
                .returnResult()
                .getResponseBody();
    }

    private LoginSuccessResponseDTO loginExpectingSuccess(String login, String password) {
        var loginRequest = Map.of("login", login, "password", password);
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult()
                .getResponseBody();
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

    private UUID createTestUser(String adminToken, String login, String email) {
        var addUserRequest = Map.of(
                "name", "Test",
                "surname", "User",
                "email", email,
                "login", login,
                "password", "InitialPass1!",
                "accessLevels", java.util.List.of(Map.of("levelName", "CLIENT"))
        );

        Map response = restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addUserRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        return UUID.fromString(response.get("id").toString());
    }

    private void changeUserPasswordAsAdmin(String adminToken, UUID userId, String newPassword) {
        var request = Map.of("newPassword", newPassword);
        restTestClient.post()
                .uri("/accounts/" + userId + "/change-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk();
    }


    private void setNewPassword(String signature, SetNewPasswordRequestDTO dto) {

        restTestClient.post()
                .uri("/auth/forced-new-password")
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldForcePasswordChangeAfterAdminChangesPassword() {
        String adminToken = loginAsAdmin();
        String login = uniqueLogin("forceuser");
        String email = uniqueEmail("forceuser");
        UUID userId = createTestUser(adminToken, login, email);

        String newForcedPassword = "ForcedPass1!";
        changeUserPasswordAsAdmin(adminToken, userId, newForcedPassword);

        // User logs in with the forced password – only id and version
        var loginRequest = Map.of("login", login, "password", newForcedPassword);
        var firstLoginResponse = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginRequiresPasswordChange.class)
                .returnResult();

        String signature = firstLoginResponse.getResponseHeaders()
                .getFirst("X-Resource-Signature");

        LoginRequiresPasswordChange dto = firstLoginResponse.getResponseBody();


        // User sets a new password – both tokens must be returned
        String finalPassword = "FinalPassword1!";
        var forceDto = SetNewPasswordRequestDTO.builder()
                .password(finalPassword)
                .version(dto.getVersion())
                .id(dto.getId())
                .build();



        setNewPassword(signature, forceDto);

        var loginRequest2 = Map.of("login", login, "password", finalPassword);
        var firstLoginResponse2 = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest2)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult();

        assertThat(firstLoginResponse2.getResponseBody().getAccessToken()).isNotNull();
        assertThat(firstLoginResponse2.getResponseBody().getRefreshToken()).isNotNull();

    }


    @Test
    void shouldSuccessfullyLetChangeForcedPasswordFromAdmin2fa() {
        String adminToken = loginAsAdmin();

        UUID userId = UUID.fromString("722e31e2-567d-493b-a41a-ee64806ea144");

        Account account = accountRepository.findById(userId).orElseThrow();

        changeUserPasswordAsAdmin(adminToken, userId, "NewForced123!");

        assertThat(account.isTwoFaEnabled()).isTrue();


        var loginRequest = Map.of(
                "login", "twofaforcepassword",
                "password", "NewForced123!"
        );


        Login2FARequiredResponseDTO response = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Login2FARequiredResponseDTO.class)
                .returnResult()
                .getResponseBody();


        Assertions.assertNotNull(response);
        AssertionsForClassTypes.assertThat(response.isRequires2FA()).isTrue();


        UUID authSessionId = response.getAuthSessionId();
        AssertionsForInterfaceTypes.assertThat(authSessionId).isNotNull();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        AssertionsForInterfaceTypes.assertThat(messages).isNotEmpty();

        String latestMessage = messages.getFirst().id();
        String emailBody = mailpitClient.getMessagePlain(latestMessage);
        Matcher matcher = Pattern.compile("\\b\\d{8}\\b").matcher(emailBody);
        AssertionsForClassTypes.assertThat(matcher.find()).as("W mailu powinien znajdować się 8-cyfrowy kod").isTrue();
        String secretCode = matcher.group();

        var twoFaRequest = Map.of(
                "authSessionId", authSessionId,
                "token", secretCode
        );


        var tempAccessToken = restTestClient.post()
                .uri("/auth/twofa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(twoFaRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginRequiresPasswordChange.class)
                .returnResult();


        String signature = tempAccessToken.getResponseHeaders()
                .getFirst("X-Resource-Signature");

        LoginRequiresPasswordChange dto = tempAccessToken.getResponseBody();
        String finalPassword = "NewUserPass1!";
        var forceDto = SetNewPasswordRequestDTO.builder()
                .password(finalPassword)
                .version(dto.getVersion())
                .id(dto.getId())
                .build();

        setNewPassword(signature, forceDto);

        var firstLoginResponse2 = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", "twofaforcepassword", "password", "NewUserPass1!"))
                .exchange()
                .expectBody(Login2FARequiredResponseDTO.class)
                .returnResult();




    }

    @Test
    void shouldForcePasswordChangeForNewlyCreatedUserOnFirstLogin() {
        String adminToken = loginAsAdmin();
        String login = uniqueLogin("newuser");
        String email = uniqueEmail("newuser");
        UUID userId = createTestUser(adminToken, login, email);
        Account account = accountRepository.findById(userId).orElseThrow();

        // Pierwsze logowanie – wymuszona zmiana hasła
        var loginRequest = Map.of("login", login, "password", "InitialPass1!");
        var firstLoginResponse = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginRequiresPasswordChange.class)
                .returnResult();

        String signature = firstLoginResponse.getResponseHeaders().getFirst("X-Resource-Signature");
        LoginRequiresPasswordChange dto = firstLoginResponse.getResponseBody();

        // Ustawienie nowego hasła
        String finalPassword = "NewUserPass1!";
        var forceDto = SetNewPasswordRequestDTO.builder()
                .password(finalPassword)
                .version(dto.getVersion())
                .id(dto.getId())
                .build();
        setNewPassword(signature, forceDto);

        // Logowanie z nowym hasłem – sukces
        var finalLoginResponse = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", finalPassword))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult();

        assertThat(finalLoginResponse.getResponseBody().getAccessToken()).isNotNull();
        assertThat(finalLoginResponse.getResponseBody().getRefreshToken()).isNotNull();
    }

    @Test
    void shouldFailToSetNewPasswordWithWeakPassword() {
        String adminToken = loginAsAdmin();
        String login = uniqueLogin("weakuser");
        String email = uniqueEmail("weakuser");
        UUID userId = createTestUser(adminToken, login, email);
        Account account = accountRepository.findById(userId).orElseThrow();

        // Logowanie (wymuszona zmiana hasła)
        var loginResponse = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", "InitialPass1!"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginRequiresPasswordChange.class)
                .returnResult();

        String signature = loginResponse.getResponseHeaders().getFirst("X-Resource-Signature");
        LoginRequiresPasswordChange dto = loginResponse.getResponseBody();

        // Próba ustawienia słabego hasła
        var weakDto = SetNewPasswordRequestDTO.builder()
                .password("weak")
                .version(dto.getVersion())
                .id(dto.getId())
                .build();

        restTestClient.post()
                .uri("/auth/forced-new-password")
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(weakDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void
    shouldNotLetChangePasswordIfDontHaveFlagRequiresNewPassword() {
        // Użytkownik "accountforfpwdhch" nie ma flagi requiredNewPassword
        // Logowanie normalne (powinno zwrócić pełne tokeny)
        var loginResponse = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", "accountforfpwdhch", "password", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginSuccessResponseDTO.class)
                .returnResult();

        LoginSuccessResponseDTO success = loginResponse.getResponseBody();
        assertThat(success.getAccessToken()).isNotNull();
        assertThat(success.getRefreshToken()).isNotNull();

        // Próba wywołania forced-new-password bez sygnatury (lub z dowolną) – powinna zwrócić błąd
        // Endpoint wymaga sygnatury i DTO z id/version. Bez nich – 400 Bad Request.
        // Ale test w oryginalnie oczekiwał OK – to było błędne. Poprawiam:
        restTestClient.post()
                .uri("/auth/forced-new-password")
                .header("Authorization", "Bearer " + success.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("password", "AnyPass1!"))
                .exchange()
                .expectStatus().isBadRequest(); // lub Forbidden – zależnie od walidacji
    }

    @Test
    void shouldFailToCallNewPasswordWithoutDTO() {
        restTestClient.post()
                .uri("/auth/forced-new-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("password", "Irrelevant1!"))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
