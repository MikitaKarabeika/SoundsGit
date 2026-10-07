// MOK.11 Dołącz poziom dostępu do konta + MOK.12 Odłącz poziom dostępu od konta

package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.common.Language;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.api.exceptions.AccountNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;





public class MOK_11_MOK12_AccessLevelTest extends MOKIntegrationTestBase {


    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    MailpitClient mailpitClient = mailpit.getClient();

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager transactionManager;



    @Test
    void ShouldChangeAccessLevel() {
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

        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "client10")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isOk();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "client10", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get()
                .uri("/accounts?page=0&size=50")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$..[?(@.login == 'client10')].accessLevels[?(@.levelName == 'CLIENT')].active")
                .isEqualTo(false);

        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "client10")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri("/accounts?page=0&size=50")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$..[?(@.login == 'client10')].accessLevels[?(@.levelName == 'CLIENT')].active")
                .isEqualTo(true);
    }

    @Test
    void ShouldThrowExceptionNotFoundAccountOrRole() {
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


        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "dasdadawdwd")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isNotFound();

        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "client3")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "adsdadada"))
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "dasdadawdwd", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "client3", "adsdadada")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "", "")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", " ", " ")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isBadRequest();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "@!#@!@#!", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "dd", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();


        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeaccesslevel", "2@##@#!!")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();

        // Wielkość liter
        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeaccesslevel", "ClIeNt")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();

        // Wielkość liter
        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "rEvOkEAcceSsLeVel", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();

        // Spacji za dużo trochę
        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeaccesslevel", " CLIENT ")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();


    }
    @Test
    void ShouldSendNotificationOnEmailAboutRevokingAccessLevel(){

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


        restTestClient.get()
                .uri("/accounts/d2e44132-a345-44db-9870-a45a18d06231")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$..[?(@.login == 'revokeaccesslevel')]").exists()
                .jsonPath("$.id").isEqualTo("d2e44132-a345-44db-9870-a45a18d06231")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')].active").isEqualTo(true);


        mailpitClient.deleteAllMessages();


        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeaccesslevel", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get()
                .uri("/accounts/d2e44132-a345-44db-9870-a45a18d06231")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("revokeaccesslevel")
                .jsonPath("$.id").isEqualTo("d2e44132-a345-44db-9870-a45a18d06231")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')].active").isEqualTo(false);


        var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();
        String emailBody = mailpitClient.getMessagePlain(messages.getFirst().id());

        assertThat(emailBody).contains("zmienione, administrator odłączył poziom dostępu do twojego konta.");


        // Ustawmy język angielski
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            Account account = accountRepository.findByLogin("revokeaccesslevel")
                    .orElseThrow(AccountNotFoundException::new);

            account.setLanguage(Language.valueOf("EN"));
            accountRepository.saveAndFlush(account);
        });

        mailpitClient.deleteAllMessages();

        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "revokeaccesslevel")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isOk();

        mailpitClient.deleteAllMessages();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeaccesslevel", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();



        restTestClient.get()
                .uri("/accounts/d2e44132-a345-44db-9870-a45a18d06231")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("revokeaccesslevel")
                .jsonPath("$.id").isEqualTo("d2e44132-a345-44db-9870-a45a18d06231")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')].active").isEqualTo(false);



        var messages2 = mailpitClient.getAllMessages();
        assertThat(messages2).isNotEmpty();
        String emailBody2 = mailpitClient.getMessagePlain(messages2.getFirst().id());


        assertThat(emailBody2).contains("changed, admin revoked access level of your account.");


    }

    @Test
    void ShouldRevokeIfAlreadyRevokedButWithoutSendingNotification(){
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


        restTestClient.get()
                .uri("/accounts/e2e44132-a345-44db-9870-a45a18d06231")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("revokeaccessleveltwo")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')].active").isEqualTo(false);

        mailpitClient.deleteAllMessages();


        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeaccessleveltwo", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get()
                .uri("/accounts/e2e44132-a345-44db-9870-a45a18d06231")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("revokeaccessleveltwo")
                .jsonPath("$.accessLevels[?(@.levelName == 'CLIENT')].active").isEqualTo(false);



        var messages2 = mailpitClient.getAllMessages();
        assertThat(messages2).isEmpty();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeaccessleveltwo", "ADMIN")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();

    }

    @Test
    void ShouldNotRevokeOwnAdminRoleIfYouAreAdmin() {
        var loginRequest = Map.of(
                "login", "revokeadmin",
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

        // Nie może zabrać sobie rolę admina
        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeadmin", "ADMIN")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isBadRequest();

        var loginRequest2 = Map.of(
                "login", "admin1",
                "password", "password"
        );

        String adminToken2 = restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest2)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();


        restTestClient.get()
                .uri("/accounts/722e31e2-567d-493b-a41a-cf64806ea144")
                .header("Authorization", "Bearer " + adminToken2)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("revokeadmin")
                .jsonPath("$.id").isEqualTo("722e31e2-567d-493b-a41a-cf64806ea144")
                .jsonPath("$.accessLevels[?(@.levelName == 'ADMIN')].active").isEqualTo(true);





        // Inny admin może zabrać rolę innego admina
        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "revokeadmin", "ADMIN")
                .header("Authorization", "Bearer " + adminToken2)
                .exchange()
                .expectStatus().isNoContent();


        restTestClient.get()
                .uri("/accounts/722e31e2-567d-493b-a41a-cf64806ea144")
                .header("Authorization", "Bearer " + adminToken2)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Resource-Signature")
                .expectBody()
                .jsonPath("$.login").isEqualTo("revokeadmin")
                .jsonPath("$.id").isEqualTo("722e31e2-567d-493b-a41a-cf64806ea144")
                .jsonPath("$.accessLevels[?(@.levelName == 'ADMIN')].active").isEqualTo(false);


    }

    @Test
    void ShouldSendNotificationOnEmailAboutAddingAndGrantingAccessLevel(){

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

        mailpitClient.deleteAllMessages();


        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "addaccessleveltest")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isOk();



            var messages = mailpitClient.getAllMessages();
        assertThat(messages).isNotEmpty();
        String emailBody = mailpitClient.getMessagePlain(messages.getFirst().id());

        assertThat(emailBody).contains("zmienione, administrator dodał poziom dostępu do twojego konta.");

        System.out.println(emailBody);

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "grantaccessleveltest", "CLIENT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        mailpitClient.deleteAllMessages();


        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "grantaccessleveltest")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isOk();

        var messages2 = mailpitClient.getAllMessages();
        assertThat(messages2).isNotEmpty();
        String emailBody2 = mailpitClient.getMessagePlain(messages2.getFirst().id());

        assertThat(emailBody2).contains("changed, administrator granted access level to your account.");

        System.out.println(emailBody2);
    }

    @Test
    void ClientUnauthorizedAccessToAccessLevel() {
        var loginRequest = Map.of(
                "login", "client3",
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

        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "client3")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "client3")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "ADMIN"))
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "client3", "CLIENT")
                .header("Authorization", "Bearer " + clientToken)
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "client3", "ADMIN")
                .header("Authorization", "Bearer " + clientToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void GuestUnauthorizedAccessToAccessLevel() {
        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "employee2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "CLIENT"))
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "employee2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "ADMIN"))
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "admin1", "ADMIN")
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "client2", "CLIENT")
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "client2", "dsds")
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "dasdas", "ADMIN")
                .exchange()
                .expectStatus().isUnauthorized();

        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "dsadas", "dsds")
                .exchange()
                .expectStatus().isUnauthorized();
    }


}
