package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.token.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {
    Optional<ActivationToken> findByToken(String token);
}
