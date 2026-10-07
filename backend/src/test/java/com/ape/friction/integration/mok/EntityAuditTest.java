package com.ape.friction.integration.mok;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import   com.ape.friction.api.dto.data.EditUserAccountDataDataDTO;
import   com.ape.friction.api.dto.response.account.AccountResponseDTO;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityAuditTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    private record TestAccountListDTO(UUID id, String login) {}


    private Map<String, Object> validRequest(String email, String login) {
        return Map.of(
                "name", "Jan",
                "surname", "Kowalski",
                "email", email,
                "login", login,
                "password", "Password1!",
                "accessLevels", List.of(Map.of("levelName", "CLIENT"))
        );
    }

    @Test
    public void shouldHaveCreateByThenUpdatedBy() {
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



        var response = restTestClient.post()
                .uri("/accounts/add-user")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validRequest("newuser@example.com", "newuser"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AccountResponseDTO.class)
                .returnResult();


        UUID id = response.getResponseBody().getId();

        restTestClient.get()
                .uri("/accounts/audit/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.createdBy").isEqualTo("admin1")
                .jsonPath("$.lastModifiedBy").isEqualTo("admin1");



        var response2 = restTestClient.get()
                .uri("/accounts/" +  id)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EditUserAccountDataDataDTO.class)
                .returnResult();


        EditUserAccountDataDataDTO dto = response2.getResponseBody();

        String signature = response2.getResponseHeaders()
                .getFirst("X-Resource-Signature");


        EditUserAccountDataDataDTO updated = EditUserAccountDataDataDTO.builder()
                .name("Norbert")
                .id(UUID.fromString("f7f9a627-42ed-4c19-8707-8d21456a2194"))
                .surname(dto.surname())
                .version(dto.version())
                .build();


        restTestClient.patch()
                .uri("/accounts/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .header("X-Resource-Signature", signature)
                .body(updated)
                .exchange()
                .expectStatus().isOk();


        restTestClient.get()
                .uri("/accounts/audit/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.createdBy").isEqualTo("admin1")
                .jsonPath("$.lastModifiedBy").isEqualTo("admin1");

        ;

    }


    @Test
    public void shouldNotHaveCreateBy() throws IOException {
        var request = validRequest("costam@gmail.com", "login");

        restTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated();


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

        String responseJson = restTestClient.get()
                .uri("/accounts?page=0&size=70")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        List<String> ids = JsonPath.read(responseJson, "$..[?(@.login == 'login')].id");
        UUID id = UUID.fromString(ids.get(0));


        restTestClient.get()
                .uri("/accounts/audit/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.createdBy").isEqualTo(null)
                .jsonPath("$.lastModifiedBy").isEqualTo(null);


    }

}
