package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.dto.response.projection.LoginAuditProjection;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.entities.users.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY, transactionManager = "mokTransactionManager")
public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID> {
    Optional<LoginAudit> findByAccount(Account account);

    @Query("""
    SELECT la.lastLoginAt AS lastLoginAt,
           la.lastSuccessfulAuthIp AS lastSuccessfulAuthIp,
           la.lastFailedAuthTime AS lastFailedAuthTime,
           la.lastFailedAuthIp AS lastFailedAuthIp
    FROM LoginAudit la
    WHERE la.account.id = :accountId
""")
    LoginAuditProjection findProjectedByAccountId(@Param("accountId") UUID accountId);
}
