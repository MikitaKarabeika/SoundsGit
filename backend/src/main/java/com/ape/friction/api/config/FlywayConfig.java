package com.ape.friction.api.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@Profile("!test")
public class FlywayConfig {

    @Bean
    @Primary
    @ConfigurationProperties("flyway.datasource")
    public DataSourceProperties flywayDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "flywayDataSource")
    @Primary
    public DataSource flywayDataSource() {
        return flywayDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "flyway")
    public Flyway flyway(
            @Qualifier("flywayDataSource") DataSource dataSource,
            Environment environment) {
        System.out.println("====== [FLYWAY STARTING] ======");

        String mokUser = environment.getProperty("POSTGRES_MOK_USER", "mok_user");
        String mofUser = environment.getProperty("POSTGRES_MOF_USER", "mof_user");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .placeholders(Map.of(
                        "mok_user", mokUser,
                        "mof_user", mofUser
                ))
                .baselineOnMigrate(true)
                .load();

        var result = flyway.migrate();
        System.out.println("====== [FLYWAY SUCCESS: " + result.migrationsExecuted + " MIGRATIONS APPLIED] ======");

        return flyway;
    }
}