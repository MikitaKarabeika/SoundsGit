package com.ape.friction.api.config;

import jakarta.persistence.EntityManagerFactory;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.hibernate.integration.HibernateUnproxyObjectAccessHook;
import org.javers.repository.sql.ConnectionProvider;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.JaversSqlRepository;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.javers.spring.jpa.JpaHibernateConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaversConfig {

    @Bean
    public Javers javers(@Qualifier("mokEntityManagerFactory") EntityManagerFactory emf) {

        ConnectionProvider connectionProvider = new JpaHibernateConnectionProvider(emf.createEntityManager());

        JaversSqlRepository sqlRepository = SqlRepositoryBuilder
                .sqlRepository()
                .withConnectionProvider(connectionProvider)
                .withDialect(DialectName.POSTGRES)
                .build();

        return JaversBuilder.javers()
                .registerJaversRepository(sqlRepository)
                .withObjectAccessHook(new HibernateUnproxyObjectAccessHook())
                .build();
    }
}
