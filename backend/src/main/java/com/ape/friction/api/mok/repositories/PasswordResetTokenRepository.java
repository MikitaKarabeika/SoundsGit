package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.token.PasswordResetToken;
import com.ape.friction.api.entities.users.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByAccount(Account account);
}
