package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.token.EmailChangeToken;
import com.ape.friction.api.entities.users.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, UUID> {

    Optional<EmailChangeToken> findByToken(String token);

    Optional<EmailChangeToken> findByAccount(Account account);
}
