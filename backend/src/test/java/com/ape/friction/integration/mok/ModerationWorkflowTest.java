package com.ape.friction.integration.mok;

import com.ape.friction.integration.config.MOKIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

class ModerationWorkflowTest extends MOKIntegrationTestBase {

    private static final String REPORTER_ID = "b1e44132-a345-44db-9870-a45a18d0665f";
    private static final String REPORTED_ACCOUNT_ID = "034fd304-8b55-4ea2-a3c0-f35dadb717c5";

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void reportCanBeSubmittedReviewedAndRecordedInDecisionLog() {
        String adminToken = login("admin1");
        restTestClient.put()
                .uri("/accounts/{login}/access-levels", "client10")
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("levelName", "MODERATOR"))
                .exchange()
                .expectStatus().isOk();
        restTestClient.delete()
                .uri("/accounts/{login}/access-levels/{levelName}", "client10", "CLIENT")
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isNoContent();

        String clientToken = login("client1");
        String reportedUserToken = login("client3");
        String reportId = restTestClient.post()
                .uri("/moderation/reports")
                .headers(headers -> headers.setBearerAuth(clientToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "reportedAccountId", REPORTED_ACCOUNT_ID,
                        "category", "SPAM",
                        "details", "Repeated unsolicited messages"
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("id").toString();

        String moderatorToken = login("client10");
        restTestClient.get()
                .uri("/accounts/me")
                .headers(headers -> headers.setBearerAuth(moderatorToken))
                .exchange()
                .expectStatus().isOk();
        restTestClient.get()
                .uri("/moderation/reports")
                .headers(headers -> headers.setBearerAuth(moderatorToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].id").isEqualTo(reportId)
                .jsonPath("$.content[0].reporterAccountId").isEqualTo(REPORTER_ID)
                .jsonPath("$.content[0].reportedLogin").isEqualTo("client3");

        restTestClient.post()
                .uri("/moderation/reports/{reportId}/decision", reportId)
                .headers(headers -> headers.setBearerAuth(moderatorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "outcome", "CONFIRMED",
                        "note", "Confirmed after review",
                        "sanction", "TIMEOUT",
                        "durationHours", 24
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.moderatorLogin").isEqualTo("client10")
                .jsonPath("$.outcome").isEqualTo("CONFIRMED")
                .jsonPath("$.sanction").isEqualTo("TIMEOUT");

        restTestClient.get()
                .uri("/accounts/me")
                .headers(headers -> headers.setBearerAuth(reportedUserToken))
                .exchange()
                .expectStatus().isForbidden();
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", "client3", "password", "password"))
                .exchange()
                .expectStatus().is4xxClientError();

        restTestClient.get()
                .uri("/moderation/reports")
                .headers(headers -> headers.setBearerAuth(moderatorToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0);

        restTestClient.get()
                .uri("/moderation/decisions")
                .headers(headers -> headers.setBearerAuth(moderatorToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].reportId").isEqualTo(reportId)
                .jsonPath("$.content[0].note").isEqualTo("Confirmed after review");

        restTestClient.get()
                .uri("/moderation/accounts/{accountId}", REPORTED_ACCOUNT_ID)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.moderationStatus").isEqualTo("TIMED_OUT")
                .jsonPath("$.reportId").isEqualTo(reportId);

        restTestClient.post()
                .uri("/moderation/accounts/{accountId}/lift", REPORTED_ACCOUNT_ID)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("note", "Timeout reviewed by admin"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.moderationStatus").isEqualTo("ACTIVE");
        restTestClient.get()
                .uri("/accounts/me")
                .headers(headers -> headers.setBearerAuth(reportedUserToken))
                .exchange()
                .expectStatus().isOk();

        restTestClient.post()
                .uri("/moderation/accounts/{accountId}/ban", REPORTED_ACCOUNT_ID)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("note", "Repeated severe abuse"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.moderationStatus").isEqualTo("BANNED");
        restTestClient.get()
                .uri("/moderation/accounts/{accountId}/actions", REPORTED_ACCOUNT_ID)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].actionType").isEqualTo("PERMANENT_BAN");
        restTestClient.get()
                .uri("/accounts/me")
                .headers(headers -> headers.setBearerAuth(reportedUserToken))
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.post()
                .uri("/moderation/accounts/{accountId}/lift", REPORTED_ACCOUNT_ID)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("note", "Ban reversed after appeal"))
                .exchange()
                .expectStatus().isOk();

        restTestClient.post()
                .uri("/moderation/reports/{reportId}/decision", reportId)
                .headers(headers -> headers.setBearerAuth(moderatorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("outcome", "DISMISSED", "note", "Second decision"))
                .exchange()
                .expectStatus().is4xxClientError();

        String warningReportId = restTestClient.post()
                .uri("/moderation/reports")
                .headers(headers -> headers.setBearerAuth(clientToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "reportedAccountId", REPORTED_ACCOUNT_ID,
                        "category", "OTHER",
                        "details", "Further inappropriate behavior"
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("id").toString();

        restTestClient.post()
                .uri("/moderation/reports/{reportId}/decision", warningReportId)
                .headers(headers -> headers.setBearerAuth(moderatorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("outcome", "CONFIRMED", "note", "Confirmed; warning issued"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sanction").isEqualTo("WARNING");
        restTestClient.get()
                .uri("/moderation/accounts/{accountId}", REPORTED_ACCOUNT_ID)
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.moderationStatus").isEqualTo("WARNED");
    }

    @Test
    void usersCannotReviewReportsAndCannotReportTheirOwnAccount() {
        String clientToken = login("client1");

        restTestClient.get()
                .uri("/moderation/reports")
                .headers(headers -> headers.setBearerAuth(clientToken))
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.get()
                .uri("/moderation/accounts")
                .headers(headers -> headers.setBearerAuth(clientToken))
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.post()
                .uri("/moderation/accounts/{accountId}/ban", REPORTED_ACCOUNT_ID)
                .headers(headers -> headers.setBearerAuth(clientToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("note", "Not allowed"))
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.post()
                .uri("/moderation/reports")
                .headers(headers -> headers.setBearerAuth(clientToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "reportedAccountId", REPORTER_ID,
                        "category", "OTHER",
                        "details", "Self-report"
                ))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private String login(String login) {
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken").toString();
    }
}
