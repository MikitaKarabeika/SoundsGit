package com.ape.friction.integration.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@TestConfiguration
@EnableJpaRepositories(
        basePackages = "com.ape.friction.api.mok",
        entityManagerFactoryRef = "mokEntityManagerFactory",
        transactionManagerRef = "mokTransactionManager"
)
public class TestMokJpaConfig {
    @Bean
    public JdbcTemplate mokJdbcTemplate(
            @Qualifier("mokDataSource")
            DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }
}
