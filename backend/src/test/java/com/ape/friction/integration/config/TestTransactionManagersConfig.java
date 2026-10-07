package com.ape.friction.integration.config;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import jakarta.persistence.EntityManagerFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@TestConfiguration(proxyBeanMethods = false)
public class TestTransactionManagersConfig {

    @Bean
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
        return new EntityManagerFactoryBuilder(
                new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter(),
                (dataSource) -> new HashMap<String, Object>(),
                null
        );
    }

    @Bean(name = "entityManagerFactory")
    public EntityManagerFactory entityManagerFactory(
            @Qualifier("mokEntityManagerFactory") LocalContainerEntityManagerFactoryBean mokEmf) {
        return Objects.requireNonNull(mokEmf.getObject());
    }


    @Bean(name = "mokDataSource")
    public DataSource mokDataSource() {
        PostgreSQLContainer pg = IntegrationTestBase.postgres;
        return DataSourceBuilder.create()
                .url(pg.getJdbcUrl() )
                .username(pg.getUsername())
                .password(pg.getPassword())
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Primary
    @Bean(name = "mokEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mokEntityManagerFactory(
            @Qualifier("mokDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder) {

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "create-drop");

        jpaProperties.put("hibernate.hbm2ddl.import_files", "data.sql");
        jpaProperties.put("hibernate.hbm2ddl.import_files_sql_extractor", "org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor");
        jpaProperties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

        return builder
                .dataSource(dataSource)
                .packages(
                        "com.ape.friction.api.entities.users",
                        "com.ape.friction.api.entities.token",
                        "com.ape.friction.api.entities.password",
                        "com.ape.friction.api.entities.errorTranslation",
                        "com.ape.friction.api.entities.moderation"
                )
                .persistenceUnit("mokPersistenceUnit")
                .properties(jpaProperties)
                .build();
    }

    @Bean(name = "mokTransactionManager")
    @Primary
    public PlatformTransactionManager mokTransactionManager(
            @Qualifier("mokEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(Objects.requireNonNull(emf.getObject()));
    }


    @Bean(name = "mocDataSource")
    public DataSource mocDataSource() {
        PostgreSQLContainer pg = IntegrationTestBase.postgres;
        return DataSourceBuilder.create()
                .url(pg.getJdbcUrl())
                .username(pg.getUsername())
                .password(pg.getPassword())
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean(name = "mocEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mocEntityManagerFactory(
            @Qualifier("mocDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages(
                        "com.ape.friction.api.entities.users",
                        "com.ape.friction.api.entities.token",
                        "com.ape.friction.api.entities.password",
                        "com.ape.friction.api.entities.errorTranslation"
                )
                .persistenceUnit("mocPersistenceUnit")
                .build();
    }

    @Bean(name = "mocTransactionManager")
    public PlatformTransactionManager mocTransactionManager(
            @Qualifier("mocEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(Objects.requireNonNull(emf.getObject()));
    }

    @Bean(name = "mokDatabaseConnection")
    public DatabaseDataSourceConnectionFactoryBean mokDatabaseConnection(
            @Qualifier("mokDataSource") DataSource dataSource) {
        return createConnectionFactory(dataSource);
    }

    @Bean(name = "mocDatabaseConnection")
    public DatabaseDataSourceConnectionFactoryBean mocDatabaseConnection(
            @Qualifier("mocDataSource") DataSource dataSource) {
        return createConnectionFactory(dataSource);
    }

    private DatabaseDataSourceConnectionFactoryBean createConnectionFactory(DataSource dataSource) {
        DatabaseDataSourceConnectionFactoryBean factory = new DatabaseDataSourceConnectionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setSchema("public");

        DatabaseConfigBean config = new DatabaseConfigBean();
        config.setDatatypeFactory(new PostgresqlDataTypeFactory());
        factory.setDatabaseConfig(config);
        return factory;
    }

}
