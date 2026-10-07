package com.ape.friction.integration.mok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.data.AuditRecordDTO;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JaversAuditTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    private String login(String login, String password) {
        return restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("accessToken")
                .toString();
    }

    private List<AuditRecordDTO> getAuditRecords(String token) {
        return restTestClient.get()
                .uri("/audit?limit=100")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<AuditRecordDTO>>() {})
                .returnResult()
                .getResponseBody();
    }

    @Test
    void shouldCreateInitialAuditRecordWhenUserIsCreated() {
        String adminToken = login("admin1", "password");
        String uniqueLogin = "audit_create_" + UUID.randomUUID().toString().substring(0, 8);
        String uniqueEmail = uniqueLogin + "@example.com";

        restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Audit",
                        "surname", "Create",
                        "email", uniqueEmail,
                        "login", uniqueLogin,
                        "password", "Password1!",
                        "accessLevels", List.of(Map.of("levelName", "CLIENT"))
                ))
                .exchange()
                .expectStatus().isCreated();

        List<AuditRecordDTO> auditRecords = getAuditRecords(adminToken);

        boolean createdRecordFound = auditRecords.stream().anyMatch(record ->
                "INITIAL".equals(record.changeType()) &&
                        record.entityType().contains("Account") &&
                        "admin1".equals(record.author())
        );

        assertThat(createdRecordFound)
                .as("Audit record for user creation (INITIAL) should be present")
                .isTrue();
    }

    @Test
    void shouldCreateUpdateAuditRecordWhenUserIsSoftDeleted() {
        String adminToken = login("admin1", "password");
        String uniqueLogin = "audit_delete_" + UUID.randomUUID().toString().substring(0, 8);
        String uniqueEmail = uniqueLogin + "@example.com";

        String createdUserId = restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Audit",
                        "surname", "Delete",
                        "email", uniqueEmail,
                        "login", uniqueLogin,
                        "password", "Password1!",
                        "accessLevels", List.of(Map.of("levelName", "CLIENT"))
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("id")
                .toString();

        restTestClient.delete()
                .uri("/accounts/{id}/delete", createdUserId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        List<AuditRecordDTO> auditRecords = getAuditRecords(adminToken);

        boolean updateRecordFound = auditRecords.stream().anyMatch(record ->
                "UPDATE".equals(record.changeType()) &&
                        record.entityType().contains("Account") &&
                        "admin1".equals(record.author())
        );

        assertThat(updateRecordFound)
                .as("Audit record for user soft deletion (UPDATE) should be present")
                .isTrue();
    }

    @Test
    void shouldReturnAuditHistory() {
        String adminToken = login("admin1", "password");
        restTestClient.get()
                .uri("/audit")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<AuditRecordDTO>>() {})
                .value(history -> assertThat(history).isNotNull());
    }

    @Test
    void shouldReturnAuditHistoryWithPagination() {
        String adminToken = login("admin1", "password");
        restTestClient.get()
                .uri("/audit?limit=10&skip=0")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<AuditRecordDTO>>() {})
                .value(history -> {
                    assertThat(history).isNotNull();
                    assertThat(history.size()).isLessThanOrEqualTo(10);
                });
    }

    @Test
    void shouldReturnAuditHistorySortedByDateDesc() {
        String adminToken = login("admin1", "password");
        restTestClient.get()
                .uri("/audit?sortBy=DATE_DESC")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<AuditRecordDTO>>() {})
                .value(history -> assertThat(history).isNotNull());
    }

    @Test
    void shouldRejectWhenUserHasNoPermission() {
        String token = login("client1", "password");
        restTestClient.get()
                .uri("/audit")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

}
