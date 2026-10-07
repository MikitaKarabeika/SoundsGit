package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.dto.response.login.LoginCodeResponseDTO;
import com.ape.friction.api.entities.token.LoginCode;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.exceptions.AccountNotFoundException;
import com.ape.friction.api.exceptions.BadLoginCredentialsException;
import com.ape.friction.api.exceptions.DisabledAccountException;
import com.ape.friction.api.exceptions.NotVerifiedAccountException;
import com.ape.friction.api.exceptions.tokenAndLoginCodes.CodeExpiredException;
import com.ape.friction.api.exceptions.tokenAndLoginCodes.CodeNotExpiredException;
import com.ape.friction.api.exceptions.tokenAndLoginCodes.CodeNotFoundException;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.LoginCodeRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW,  readOnly = true, transactionManager = "mokTransactionManager")
public class LoginCodeService {

    private final AccountRepository accountRepository;
    private final MailService mailService;
    private final LoginCodeRepository loginCodeRepository;
    private final JwtService jwtService;
    private final SecureRandom random;
    private final PasswordEncoder passwordEncoder;


    private static final String CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int CODE_LENGTH = 10;



    @Transactional(propagation = Propagation.REQUIRES_NEW,  readOnly = false, transactionManager = "mokTransactionManager")
    public LoginCodeResponseDTO sendCodeLink(String email) {

        UUID authSessionId = UUID.randomUUID();


         accountRepository.findByEmailIgnoreCase(email)
                .filter(Account::isActive)
                .filter(Account::isVerified)
                .ifPresent(account -> {

                    loginCodeRepository.findByAccount(account).ifPresent(existingCode -> {

                        Optional.of(existingCode)
                                .filter(c -> c.getExpiresDate().isAfter(LocalDateTime.now()))
                                .ifPresent(c -> { throw new CodeNotExpiredException(); });

                        loginCodeRepository.delete(existingCode);
                        loginCodeRepository.flush();
                    });

                    StringBuilder codeBuilder = new StringBuilder(10);

                    for (int i = 0; i < CODE_LENGTH; i++) {
                        codeBuilder.append(CHARS.charAt(random.nextInt(CHARS.length())));
                    }
                    String secretCode = codeBuilder.toString();



                    LoginCode loginCode = new LoginCode();
                    loginCode.setCode(passwordEncoder.encode(secretCode));
                    loginCode.setAuthSessionId(authSessionId);
                    loginCode.setAccount(account);

                    loginCode.setExpiresDate(LocalDateTime.now().plusMinutes(5));

                    loginCodeRepository.saveAndFlush(loginCode);

                    mailService.sendLoginCode(
                            account.getEmail(),
                            secretCode,
                            Locale.forLanguageTag(account.getLanguage().toString())
                    );

                });

        return LoginCodeResponseDTO.builder()
                .authSessionId(authSessionId)
                .build();

    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            readOnly = false,
            transactionManager = "mokTransactionManager",
            noRollbackFor = CodeExpiredException.class
    )
    public List<String> loginWithCode(String code, UUID authSessionId) {

        LoginCode loginCode = loginCodeRepository.findByAuthSessionId(authSessionId)
                .orElseThrow(CodeNotFoundException::new);

        Account account =loginCode.getAccount();

        if(account == null)
        {
         throw new AccountNotFoundException();
        }
        if (!account.isActive()) {
            throw new DisabledAccountException();
        }
        if (!account.isVerified()) {
            throw new NotVerifiedAccountException();
        }


        if (!passwordEncoder.matches(code, loginCode.getCode())) {
            throw new BadLoginCredentialsException();
        }

        if (loginCode.getExpiresDate().isBefore(LocalDateTime.now())) {
            loginCodeRepository.delete(loginCode);
            loginCodeRepository.flush();
            throw new CodeExpiredException();
        }

        loginCodeRepository.delete(loginCode);
        loginCodeRepository.flush();

        return jwtService.authenticate(account,2);
    }


}


