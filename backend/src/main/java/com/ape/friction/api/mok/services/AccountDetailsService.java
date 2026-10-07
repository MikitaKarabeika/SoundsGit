package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.dto.response.LoginAuditDTO;
import com.ape.friction.api.dto.response.projection.LoginAuditProjection;
import com.ape.friction.api.dto.response.projection.TimezoneResponseProjection;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.exceptions.AccountNotFoundException;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.LoginAuditRepository;
import com.ape.friction.api.mok.repositories.MOKRefreshRepository;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
public class AccountDetailsService {

    private final AccountRepository accountRepository;

    private final MOKRefreshRepository MOKRefreshRepository;

    private final LoginAuditRepository loginAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('CHANGE_TZONE')")
    public String updateMyTimeZone(String targetLogin, String newTimeZone) {
        Account target = accountRepository.findByLogin(targetLogin)
                .orElseThrow(AccountNotFoundException::new);
        target.setTimezone(newTimeZone);
        accountRepository.saveAndFlush(target);
        return newTimeZone;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('GET_TZONE')")
    public TimezoneResponseProjection getMyTimeZone(UUID accountId) {
        return accountRepository.findTimeZoneProjectedById(accountId).orElseThrow(AccountNotFoundException::new);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('GET_LOGIN_AUDIT')")
    public LoginAuditDTO getMyLoginAudit(String login) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(AccountNotFoundException::new);

        return loginAuditRepository.findByAccount(account)
                .map(a -> new LoginAuditDTO(
                        a.getLastLoginAt(),
                        a.getLastSuccessfulAuthIp(),
                        a.getLastFailedAuthTime(),
                        a.getLastFailedAuthIp()
                ))
                .orElse(new LoginAuditDTO(null, null, null, null));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('GET_ANY_LOGIN_AUDIT')")
    public LoginAuditProjection getAnyLoginAudit(UUID id) {
        accountRepository.findById(id)
                .orElseThrow(AccountNotFoundException::new);

        return loginAuditRepository.findProjectedByAccountId(id);
    }

}
