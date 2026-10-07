package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccountRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager",readOnly = true)
public class InactiveAccountDeactivateService {

    @Value("${account.inactivity.limit.days}")
    private int inactivityLimitDays;

    @Autowired
    private final AccountRepository accountRepository;

    private final GenerateActivationTokenService generateActivationTokenService;


    @Scheduled(cron = "0 0 2 * * *") // każdej nocy o 2:00
    @Transactional(readOnly = false, transactionManager = "mokTransactionManager",propagation = Propagation.REQUIRES_NEW)
    public void lockInactiveAccounts() {
        Instant cutoff = Instant.now().minus(inactivityLimitDays, ChronoUnit.DAYS);

        List<Account> toBlock = accountRepository
                .findAllByActiveTrueAndLastLoginAtBefore(cutoff);

        for (Account account : toBlock) {
            account.setActive(false);
            accountRepository.saveAndFlush(account);

            generateActivationTokenService.generateTokenAndSendEmail(account);

        }
    }
}
