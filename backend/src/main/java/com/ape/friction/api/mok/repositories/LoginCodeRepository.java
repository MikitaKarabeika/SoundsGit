package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.token.LoginCode;
import com.ape.friction.api.entities.users.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface LoginCodeRepository extends JpaRepository<LoginCode, UUID> {
    Optional<LoginCode> findByCode(String token);

    Optional<LoginCode> findByAccount(Account account);

    Optional<LoginCode> findByAuthSessionId(UUID authSessionId);

}
