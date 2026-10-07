package com.ape.friction.integration.config;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@Import({TestTransactionManagersConfig.class, TestMokJpaConfig.class, TestTranslationConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public abstract class IntegrationTestBase {

    @Autowired
    @Qualifier("mokJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    public static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.3")
    )
            .withDatabaseName("testdb")
            .withUsername("test_user")
            .withPassword("test_password");

    @ServiceConnection
    public static final MailpitContainer mailpit = new MailpitContainer();

    static {
        postgres.start();
        mailpit.start();
    }

    @DynamicPropertySource
    public static void postgresProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = postgres.getJdbcUrl();
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.admin.url", () -> jdbcUrl);
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        registry.add("spring.datasource.admin.username", () -> username);
        registry.add("spring.datasource.admin.password", () -> password);

        registry.add("spring.datasource.moc.url", () -> jdbcUrl);
        registry.add("spring.datasource.moc.username", () -> username);
        registry.add("spring.datasource.moc.password", () -> password);

        registry.add("spring.datasource.mok.url", () -> jdbcUrl);
        registry.add("spring.datasource.mok.username", () -> username);
        registry.add("spring.datasource.mok.password", () -> password);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @DynamicPropertySource
    public static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
    }


}
