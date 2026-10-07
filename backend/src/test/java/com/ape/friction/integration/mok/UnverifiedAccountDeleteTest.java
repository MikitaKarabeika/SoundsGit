package com.ape.friction.integration.mok;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import   com.ape.friction.api.entities.users.Account;
import   com.ape.friction.api.exceptions.tokenAndLoginCodes.TokenNotFoundException;
import   com.ape.friction.integration.config.MOKIntegrationTestBase;
import   com.ape.friction.api.mok.repositories.AccountRepository;
import   com.ape.friction.api.mok.repositories.VerificationTokenRepository;
import   com.ape.friction.api.mok.services.UnverifiedAccountDeleteService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class UnverifiedAccountDeleteTest extends MOKIntegrationTestBase {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UnverifiedAccountDeleteService unverifiedAccountDeleteService;

    @Autowired
    @Qualifier("mokTransactionManager")
    private PlatformTransactionManager mokTransactionManager;

    MailpitClient mailpitClient = mailpit.getClient();

    @Test
    void shouldSendReminderEmailAndVerifyAccountUsingTokenFromReminderEmail() {
        Object originalValidityHours = ReflectionTestUtils.getField(
                unverifiedAccountDeleteService,
                "verificationTokenValidityHours"
        );

        try {
            ReflectionTestUtils.setField(
                    unverifiedAccountDeleteService,
                    "verificationTokenValidityHours",
                    0L
            );

            String suffix = UUID.randomUUID().toString().substring(0, 8);
            String email = "reminder-" + suffix + "@example.com";
            String login = "reminder" + suffix;

            mailpitClient.deleteAllMessages();

            var registerRequest = Map.of(
                    "name", "Jan",
                    "surname", "Kowalski",
                    "email", email,
                    "login", login,
                    "password", "Password1!"
            );

            restTestClient.post()
                    .uri("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(registerRequest)
                    .exchange()
                    .expectStatus().isCreated();

            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        var messages = mailpitClient.getAllMessages().stream()
                                .filter(message -> message.to().stream()
                                        .anyMatch(to -> email.equals(to.address())))
                                .toList();

                        assertThat(messages).isNotEmpty();
                    });

            var registerMessages = mailpitClient.getAllMessages().stream()
                    .filter(message -> message.to().stream()
                            .anyMatch(to -> email.equals(to.address())))
                    .toList();

            assertThat(registerMessages).isNotEmpty();

            String registerMessageId = registerMessages.getFirst().id();
            String registerEmailText = mailpitClient.getMessagePlain(registerMessageId);
            String firstToken = extractTokenFromEmailText(registerEmailText);

            Account accountBeforeVerification = new TransactionTemplate(mokTransactionManager)
                    .execute(status -> accountRepository
                            .findByLogin(login)
                            .orElseThrow());

            assertFalse(accountBeforeVerification.isVerified());

            Boolean tokenExistsBeforeReminder = new TransactionTemplate(mokTransactionManager)
                    .execute(status -> verificationTokenRepository
                            .findByToken(firstToken)
                            .isPresent());

            assertTrue(Boolean.TRUE.equals(tokenExistsBeforeReminder));

            mailpitClient.deleteAllMessages();

            unverifiedAccountDeleteService.deleteExpiredUnverifiedAccounts();

            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        var messages = mailpitClient.getAllMessages().stream()
                                .filter(message -> message.to().stream()
                                        .anyMatch(to -> email.equals(to.address())))
                                .toList();

                        assertThat(messages).isNotEmpty();
                    });

            var reminderMessages = mailpitClient.getAllMessages().stream()
                    .filter(message -> message.to().stream()
                            .anyMatch(to -> email.equals(to.address())))
                    .toList();

            assertThat(reminderMessages).isNotEmpty();

            String reminderMessageId = reminderMessages.getFirst().id();
            String reminderEmailText = mailpitClient.getMessagePlain(reminderMessageId);
            String reminderToken = extractTokenFromEmailText(reminderEmailText);

            assertThat(reminderEmailText).contains("token=");
            assertThat(reminderToken).isEqualTo(firstToken);

            Boolean reminderSent = new TransactionTemplate(mokTransactionManager)
                    .execute(status -> verificationTokenRepository
                            .findByToken(reminderToken)
                            .orElseThrow()
                            .isReminderSent());

            assertTrue(Boolean.TRUE.equals(reminderSent));

            restTestClient.get()
                    .uri("/auth/verify?token=" + reminderToken)
                    .exchange()
                    .expectStatus().isOk();

            Account accountAfterVerification = new TransactionTemplate(mokTransactionManager)
                    .execute(status -> accountRepository
                            .findByLogin(login)
                            .orElseThrow());

            assertTrue(accountAfterVerification.isVerified());

            Boolean tokenExistsAfterVerification = new TransactionTemplate(mokTransactionManager)
                    .execute(status -> verificationTokenRepository
                            .findByToken(reminderToken)
                            .isPresent());

            assertFalse(Boolean.TRUE.equals(tokenExistsAfterVerification));
        } finally {
            ReflectionTestUtils.setField(
                    unverifiedAccountDeleteService,
                    "verificationTokenValidityHours",
                    originalValidityHours
            );
        }
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
