package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.dto.request.LoginCodeDTO;
import com.ape.friction.api.dto.request.TwoFARequestDTO;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.entities.users.LoginAudit;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.Authentication2FARepository;
import com.ape.friction.api.mok.repositories.LoginAuditRepository;
import com.ape.friction.api.mok.repositories.LoginCodeRepository;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager", readOnly = true)
public class LoginAttemptLoggingService {

    private final LoginAuditRepository loginAuditRepository;
    private final AccountRepository accountRepository;
    private final Authentication2FARepository authentication2FARepository;
    private final LoginCodeRepository loginCodeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager", readOnly = false)
    public void recordFailedLogin(Account account, String ip) {
        LoginAudit audit = loginAuditRepository.findByAccount(account)
                .orElse(new LoginAudit(account));
        audit.setLastFailedAuthTime(Instant.now());
        audit.setLastFailedAuthIp(ip);
        loginAuditRepository.saveAndFlush(audit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    public Optional<String> getLoginFromTwoFARequest(TwoFARequestDTO dto) {
        return authentication2FARepository.findByAuthSessionId(dto.authSessionId())
                .map(token -> token.getAccount().getLogin());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    public Optional<String> getLoginFromLoginCode(LoginCodeDTO dto) {
        return loginCodeRepository.findByAuthSessionId(dto.authSessionId())
                .map(token -> token.getAccount().getLogin());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    public Optional<Account> findAccountByLogin(String login) {
        return accountRepository.findByLogin(login);
    }
}
