// MOK.16 Zmiana adresu e-mail innego konta

package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.dto.request.ChangeEmailRequestDTO;
import   com.ape.friction.api.entities.token.EmailChangeToken;
import   com.ape.friction.api.exceptions.tokenAndLoginCodes.TokenNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.EmailChangeTokenRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;


public class ChangeEmailOfOtherAccountTest extends MOKIntegrationTestBase {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    MailpitClient mailpitClient = mailpit.getClient();

    @Autowired
    private EmailChangeTokenRepository changeEmailTokenRepository;
    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearMailbox() {
        mailpitClient.deleteAllMessages();
    }

    @Test
    void ShouldChangeAdminEmailOfOtherAccountAndGoBackToOldOne() {
        UUID adminId = UUID.fromString("722e31e2-7e18-493b-a41a-cf64806ea144");

        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String loginToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        var getResponse = restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a2224")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");

        Map<String, Object> accountData = getResponse.getResponseBody();
        long currentVersion = Long.parseLong(accountData.get("version").toString());

        ChangeEmailRequestDTO emailRequest = new ChangeEmailRequestDTO(
                currentVersion,
                adminId,
                "cos.tam@example.com",
                "cos.tam2@example.com"
        );
        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a2224")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(emailRequest)
                .exchange()
                .expectStatus().isOk();


        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);
        System.out.println("=== EMAIL TEXT ===");
        System.out.println(emailText);
        System.out.println("=== END EMAIL ===");
        String token = extractTokenFromEmailText(emailText);

        var resetEmailRequest = Map.of(
                "token", token
        );

        restTestClient.post()
                .uri("/auth/change-email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequest)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a2224")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("clientchangeemailfromadmin")
                .jsonPath("$.email").isEqualTo("cos.tam2@example.com");

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messagesNew = mailpitClient.getAllMessages();
        assertThat(messagesNew).isNotEmpty();

        String messageIdNew = messagesNew.getFirst().id();
        String emailTextNew = mailpitClient.getMessagePlain(messageIdNew);
        System.out.println("=== EMAIL TEXT ===");
        System.out.println(emailText);
        System.out.println("=== END EMAIL ===");
        String tokenNew = extractTokenFromEmailText(emailTextNew);

        var resetEmailRequestNew = Map.of(
                "token", tokenNew
        );

        restTestClient.post()
                .uri("/auth/change-email-to-old")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestNew)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a2224")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("clientchangeemailfromadmin")
                .jsonPath("$.email").isEqualTo("cos.tam@example.com");
    }

    @Test
    void AdminChangingEmailOfOtherAccountButWrongRequest() {
        UUID adminId = UUID.fromString("722e31e2-7e18-493b-a41a-cf64806ea144");

        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String loginToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        var getResponse = restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");

        Map<String, Object> accountData = getResponse.getResponseBody();
        long currentVersion = Long.parseLong(accountData.get("version").toString());


        // Zły aktualny email podany
        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "cos.tam@example.com",
                        "cos.tam2@example.com"
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        // cos.tam nalezy do konta z innym id
        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "cos.tam@example.com",
                        "cos.tam3@example.com"))
                .exchange()
                .expectStatus().isBadRequest();

        // Nowy email jest już zajęty przez inne konto
        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "bosniak@example.com",
                        "tomasz.je@example.com"
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        // te same emaile
        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "bosniak@example.com",
                        "bosniak@example.com"
                ))
                .exchange()
                .expectStatus().isBadRequest();

        // Zły format email
        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "bosniak@example.com",
                        "bosniak!WDWexample.com"
                ))
                .exchange()
                .expectStatus().isBadRequest();

        // Puste id
        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(currentVersion,
                        adminId,
                        "bosniak@example.com",
                        "cos.tam3@example.com"))
                .exchange()
                .expectStatus().isBadRequest();


    }

    @Test
    void ConcurrentChangeByAdminDeclinedForClient() {
        UUID adminId = UUID.fromString("722e31e2-7e18-493b-a41a-cf64806ea144");

        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String loginToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        var getResponse = restTestClient.get()
                .uri("/accounts/{id}", "f7f9a557-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");

        Map<String, Object> accountData = getResponse.getResponseBody();
        long currentVersion = Long.parseLong(accountData.get("version").toString());


        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a557-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "bosniakmadry@example.com",
                        "cos.tam3@example.com"
                ))
                .exchange()
                .expectStatus().isOk();

        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a557-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "bosniakmadry@example.com",
                        "cos.tam3@example.com"
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);


        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);
        System.out.println("=== EMAIL TEXT ===");
        System.out.println(emailText);
        System.out.println("=== END EMAIL ===");
        String tokenAdmin = extractTokenFromEmailText(emailText);

        UUID clientId = UUID.fromString("f7f9a557-42ed-4c19-8707-8d21456a1124");

        var loginRequestBosniak = Map.of(
                "login", "newclient",
                "password", "password"
        );

        String loginTokenBosniak = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequestBosniak)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();

        var getResponseKlient = restTestClient.get()
                .uri("/accounts/me")
                .header("Authorization", "Bearer " + loginTokenBosniak)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signatureKlient = getResponseKlient.getResponseHeaders().getFirst("X-Resource-Signature");
        long versionKlient = Long.parseLong(getResponseKlient.getResponseBody().get("version").toString());

        restTestClient.patch()
                .uri("/accounts/me/change-email")
                .header("Authorization", "Bearer " + loginTokenBosniak)
                .header("X-Resource-Signature", signatureKlient)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        versionKlient,
                        clientId,
                        "bosniakmadry@example.com",
                        "cos.tam3@example.com"
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);


        var resetEmailRequest = Map.of(
                "token", tokenAdmin
        );

        restTestClient.post()
                .uri("/auth/change-email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequest)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts/{id}", "f7f9a557-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("newclient")
                .jsonPath("$.email").isEqualTo("cos.tam3@example.com");



    }

    @Test
    void AdminChangingEmailOfOtherAccountGoodRequestButWrongToken() {
        UUID adminId = UUID.fromString("722e31e2-7e18-493b-a41a-cf64806ea144");

        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String loginToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        var getResponse = restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");

        Map<String, Object> accountData = getResponse.getResponseBody();
        long currentVersion = Long.parseLong(accountData.get("version").toString());


        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "bosniak@example.com",
                        "cos.tamno@example.com"
                ))
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !mailpitClient.getAllMessages().isEmpty());

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();

        String messageId = messages.getFirst().id();
        String emailText = mailpitClient.getMessagePlain(messageId);
        System.out.println("=== EMAIL TEXT ===");
        System.out.println(emailText);
        System.out.println("=== END EMAIL ===");



        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(
                        currentVersion,
                        adminId,
                        "bosniak@example.com",
                        "cos.tam3@example.com"
                ))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);


        var resetEmailRequestWrong = Map.of(
                "token", "asfasfafafa"
        );

        restTestClient.post()
                .uri("/auth/change-email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestWrong)
                .exchange()
                .expectStatus().isNotFound();

        restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("clientwillnotchange")
                .jsonPath("$.email").isEqualTo("bosniak@example.com");


        //Zróbmy token przedawniony

        String token = extractTokenFromEmailText(emailText);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            EmailChangeToken tokenEntity = changeEmailTokenRepository.findByToken(token)
                    .orElseThrow(TokenNotFoundException::new);

            tokenEntity.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));

            changeEmailTokenRepository.saveAndFlush(tokenEntity);
        });

        var resetEmailRequestExpired = Map.of(
                "token", token
        );

        restTestClient.post()
                .uri("/auth/change-email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resetEmailRequestExpired)
                .exchange()
                .expectStatus().isEqualTo(410);

        restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("clientwillnotchange")
                .jsonPath("$.email").isEqualTo("bosniak@example.com");
    }

    @Test
    void GuestBadRequestChangeEmail() {
        restTestClient.post()
                .uri("/auth/change-email")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

    }

    @Test
    void ForbiddenChangeEmail() {

        UUID employeeId = UUID.fromString("25c7889a-4a5d-4b1c-9b58-443fe471e6d4");
        UUID clientId = UUID.fromString("b1e44132-a345-44db-9870-a45a18d0665f");

        var loginRequest = Map.of(
                "login", "employee1",
                "password", "password"
        );

        String loginToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)

                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(1,
                        employeeId,
                        "bosniak@example.com",
                        "cos.tam2@example.com"))
                .exchange()
                .expectStatus().isForbidden();


        var loginRequestclient = Map.of(
                "login", "client1",
                "password", "password"
        );

        String loginTokenClient = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequestclient)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginTokenClient)

                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(1,
                        clientId,
                        "bosniak@example.com",
                        "cos.tam2@example.com"))
                .exchange()
                .expectStatus().isForbidden();


    }

    @Test
    public void AdminBadVersionChangeEmailOfOtherAccount() {
        UUID adminId = UUID.fromString("722e31e2-7e18-493b-a41a-cf64806ea144");


        var loginRequest = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String loginToken = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        var getResponse = restTestClient.get()
                .uri("/accounts/{id}", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult();

        String signature = getResponse.getResponseHeaders().getFirst("X-Resource-Signature");

        Map<String, Object> accountData = getResponse.getResponseBody();
        long currentVersion = Long.parseLong(accountData.get("version").toString());


        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(currentVersion - 1,
                        adminId,
                        "bosniak1@example.com",
                        "cos.tam2@example.com"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        restTestClient.patch()
                .uri("/accounts/{id}/change-email-other-acc", "f7f9a627-42ed-4c19-8707-8d21456a1124")
                .header("Authorization", "Bearer " + loginToken)
                .header("X-Resource-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChangeEmailRequestDTO(currentVersion + 1,
                        adminId,
                        "bosniak1@example.com",
                        "cos.tam2@example.com"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
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
