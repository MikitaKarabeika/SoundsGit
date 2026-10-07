package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.common.FrontendUrlBuilder;
import com.ape.friction.api.entities.token.ActivationToken;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.ActivationTokenRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager",readOnly = true)
public class GenerateActivationTokenService {

    @Autowired
    private final ActivationTokenRepository activationTokenRepository;

    @Autowired
    private final MailService mailService;
    private final FrontendUrlBuilder frontendUrlBuilder;

    @Transactional(readOnly = false, transactionManager = "mokTransactionManager",propagation = Propagation.REQUIRES_NEW)
    public void generateTokenAndSendEmail(Account account) {


        String token = UUID.randomUUID().toString();
        ActivationToken activationToken = new ActivationToken();
        activationToken.setToken(token);
        activationToken.setAccount(account);
        activationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        activationTokenRepository.save(activationToken);

        String link = frontendUrlBuilder.withToken("/auth/activate", token);
        mailService.sendActivationMail(account.getEmail(),link,Locale.forLanguageTag(account.getLanguage().toString()));
    }



}
