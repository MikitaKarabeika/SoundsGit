package com.ape.friction.api.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.filters.userPrincipal.UserPrincipal;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@RequiredArgsConstructor
public class AuditConfig {

    @PersistenceContext(unitName = "mokPersistenceUnit")
    private EntityManager entityManager;

    @Bean
    public AuditorAware<Account> auditorAware() {
        return () -> {

            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof UserPrincipal userPrincipal) {

                Account reference = entityManager.getReference(
                        Account.class,
                        userPrincipal.id()
                );

                return Optional.of(reference);
            }

            return Optional.empty();
        };
    }
}
