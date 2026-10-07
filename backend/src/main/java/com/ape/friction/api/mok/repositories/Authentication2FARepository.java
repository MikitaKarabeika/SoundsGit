package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.token.AuthenticationToken;
import com.ape.friction.api.entities.users.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface Authentication2FARepository extends JpaRepository<AuthenticationToken, UUID> {

    Optional<AuthenticationToken> findByAuthSessionId(UUID authSessionId);

    Optional<AuthenticationToken> findByAccount(Account account);

    void deleteAllByAccountAndExpiresAtBefore(Account account, Instant now);
}
