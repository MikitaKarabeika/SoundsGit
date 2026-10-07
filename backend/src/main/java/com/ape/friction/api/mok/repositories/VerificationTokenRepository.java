package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.token.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByAccount_Login(String login);
    boolean existsByAccount_Login(String login);
    List<VerificationToken> findAllByExpiresDateBefore(Instant now);
    List<VerificationToken> findAllByReminderSentFalseAndExpiresDateAfter(Instant now);
}
