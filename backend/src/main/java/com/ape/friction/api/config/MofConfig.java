package com.ape.friction.api.config;


import jakarta.persistence.SharedCacheMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.ape.friction.api.mof",
        entityManagerFactoryRef = "mofEntityManagerFactory",
        transactionManagerRef = "mofTransactionManager"
)
//@Profile("!test")
@Profile("none")
public class MofConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.mof")
    public DataSourceProperties mofDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "mofDataSource")
    public DataSource mofDataSource() {
        return mofDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name="mofEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean mofEntityManagerFactory(
            @Qualifier("mofDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder,
            HibernateProperties hibernateProperties,
            JpaProperties jpaProperties) {

        var EMFBean = builder
                .dataSource(dataSource)
                .packages("com.ape.friction.api.entities")
                .persistenceUnit("mofPersistenceUnit")
                .build();
        EMFBean.setSharedCacheMode(SharedCacheMode.DISABLE_SELECTIVE);
        return EMFBean;
    }

    @Bean(name="mofTransactionManager")
    public PlatformTransactionManager mofTransactionManager(
            @Qualifier("mofEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }

}

