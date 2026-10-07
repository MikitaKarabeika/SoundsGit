package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.dto.response.projection.AccountListProjection;
import com.ape.friction.api.dto.response.projection.PreferencesProjection;
import com.ape.friction.api.dto.response.projection.TimezoneResponseProjection;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.interceptors.JaversUpdateRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
@Transactional(propagation = Propagation.MANDATORY)
@JaversUpdateRepository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.createdBy " +
            "LEFT JOIN FETCH a.lastModifiedBy " +
            "WHERE a.id = :id")
    Optional<Account> findByIdWithAudit(@Param("id") UUID id);

    public Account saveAndFlush(Account entity);

    Optional<Account> findByLogin(@NonNull @NotBlank @NotEmpty String login);

    boolean existsByLogin(String login);
    @Query("""
            select case when count(a) > 0 then true else false end
            from Account a
            where lower(a.email) = lower(:email)
            """)
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    Optional<Account> findByEmailIgnoreCase(@Param("email") String email);
    boolean existsByIdAndLogin(UUID id, String login);

    @Query("""
        SELECT a FROM Account a
        JOIN LoginAudit la ON la.account = a
        WHERE a.active = true AND la.lastLoginAt < :cutoff
        """)
    List<Account> findAllByActiveTrueAndLastLoginAtBefore(@Param("cutoff") Instant cutoff);

    @EntityGraph(attributePaths = {"accessLevels"})
    Page<AccountListProjection> findAllProjectedBy(Pageable pageable);

    @Query("""
        SELECT a.language AS language,
               a.theme AS theme
        FROM Account a
        WHERE a.id = :id
        """)
    Optional<PreferencesProjection> findPreferencesProjectedById(@Param("id") UUID id);

    @Query("""
        SELECT a.timezone AS timezone
        FROM Account a
        WHERE a.id = :id
""")
    Optional<TimezoneResponseProjection> findTimeZoneProjectedById(@Param("id") UUID id);
}
