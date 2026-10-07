package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.exceptions.PasswordUsedInPastException;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccountRepository;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRED , readOnly = false, transactionManager = "mokTransactionManager")
public class PasswordUpdateService {

    private final AccountRepository accountRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRED , readOnly = false, transactionManager = "mokTransactionManager")
    public void changePasswordWithHistory(Account account, String password) {

        boolean alreadyUsed = account.getPasswordHistory().stream()
                .anyMatch(history -> passwordEncoder.matches(password, history.getPassword()));

        if (alreadyUsed) {
            throw new PasswordUsedInPastException();
        }

        account.addPasswordToHistory(account.getPassword());

        account.setPassword(passwordEncoder.encode(password));
        accountRepository.saveAndFlush(account);
    }
}
