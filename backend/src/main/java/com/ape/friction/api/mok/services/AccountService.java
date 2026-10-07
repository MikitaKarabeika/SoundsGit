package com.ape.friction.api.mok.services;


import lombok.RequiredArgsConstructor;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.common.FrontendUrlBuilder;
import com.ape.friction.api.dto.data.PreferencesDataDTO;
import com.ape.friction.api.dto.response.ChangeEmailResponseDTO;
import com.ape.friction.api.dto.response.projection.AccountListProjection;
import com.ape.friction.api.dto.response.projection.PreferencesProjection;
import com.ape.friction.api.entities.token.ActivationToken;
import com.ape.friction.api.entities.token.EmailChangeToken;
import com.ape.friction.api.entities.users.*;
import com.ape.friction.api.events.AccountStatusChangedEvent;
import com.ape.friction.api.exceptions.*;
import com.ape.friction.api.exceptions.accessLevel.AccessLevelDoesntExistsException;
import com.ape.friction.api.exceptions.accessLevel.AdminCannotRevokeHisOwnAdminRole;
import com.ape.friction.api.exceptions.email.AlreadyRequestedToChangeEmailException;
import com.ape.friction.api.exceptions.email.EmailIsAlreadyTakenException;
import com.ape.friction.api.exceptions.email.EmailsAreTheSameException;
import com.ape.friction.api.exceptions.tokenAndLoginCodes.*;
import com.ape.friction.api.interceptors.JaversCreate;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mappers.AccountMapper;
import com.ape.friction.api.mok.repositories.*;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RequiredArgsConstructor
@Service
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
public class AccountService {


    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final MOKRefreshRepository MOKRefreshRepository;

    private final AccountRepository accountRepository;

    private final AccessLevelRepository accessLevelRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailChangeTokenRepository emailChangeTokenRepository;

    private final MailService mailService;

    private final FrontendUrlBuilder frontendUrlBuilder;

    private final ApplicationEventPublisher eventPublisher;

    private final AccountMapper accountMapper;

    private final ActivationTokenRepository activationTokenRepository;

    private final PasswordUpdateService passwordUpdateService;
    private final Javers javers;


    private Account getAccount(UUID id){
        Account account = accountRepository.findById(id)
                .orElseThrow(AccountNotFoundException::new);


        MOKRefreshRepository.refresh(account);

        return account;
    }

    private Account getAccountWithAudit(UUID id){
        Account account = accountRepository.findByIdWithAudit(id)
                .orElseThrow(AccountNotFoundException::new);

        MOKRefreshRepository.refresh(account);
        return account;
    }

    //MOK.4
    @Transactional(transactionManager = "mokTransactionManager", readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK4_GET_MY_ACCOUNT')")
    public Account getMyAccount(UUID id) {
       return getAccount(id);
    }

    @Transactional(transactionManager = "mokTransactionManager", readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK4_GET_MY_ACCOUNT')")
    public Account getMyAccountWithAudit(UUID id) {
        return getAccountWithAudit(id);
    }

    //MOK.5
    @Transactional(transactionManager = "mokTransactionManager", readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK5_GET_OTHER_ACCOUNT')")
    public Account getOtherAccount(UUID targetId, UUID principalId) {
        if (targetId.equals(principalId)) {
            throw new ItIsYourAccountException();
        }

        return getAccount(targetId);
    }


    @Transactional(transactionManager = "mokTransactionManager", readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK5_GET_OTHER_ACCOUNT')")
    public Account getOtherAccountWithAudit(UUID id) {
        return getAccountWithAudit(id);
    }




    //MOK.5
    @Transactional(transactionManager = "mokTransactionManager", readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @PreAuthorize("hasAuthority('MOK6_GET_ALL_ACCOUNTS')")
    public Page<AccountListProjection> getAllAccounts(Pageable pageable) {
        return accountRepository.findAllProjectedBy(pageable);
    }

    // MOK.11 Dołącz poziom dostępu do konta
    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager", readOnly = false)
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK11_GRANT_ACCESS_LEVEL')")
    public AccessLevel addExistingOrGrantAccessLevel(String login, AccessLevel newAccessLevel, String requestedLevelName) {

        String supportedLevelName = newAccessLevel instanceof Client ? "CLIENT"
                : newAccessLevel instanceof Administrator ? "ADMIN"
                : newAccessLevel instanceof Moderator ? "MODERATOR"
                : null;
        if (supportedLevelName == null || !supportedLevelName.equalsIgnoreCase(requestedLevelName)) {
            throw new AccessLevelDoesntExistsException();
        }

        Account account = accountRepository.findByLogin(login)
                .orElseThrow(AccountNotFoundException::new);

        // Juz sprawdzone było czy poprawna nazwa access level to sprawdźmy
        // czy jest dodany do konta (aktywny / nieaktywny) albo niedodany do konta
        return account.getAccessLevels().stream()
                .filter(al -> al.getLevelName().equals(requestedLevelName))
                .findFirst()
                .map(existingLevel -> {

                    if (!existingLevel.isActive()) {

                        // Dodany nieaktywny
                        existingLevel.setActive(true);
                        accountRepository.saveAndFlush(account);

                        eventPublisher.publishEvent(
                                new AccountStatusChangedEvent(account.getId(), "grantAccessLevel")
                        );

                        return existingLevel;

                    }

                    // Dodany Aktywny ale z racji, że chcemy aktywować to i tak 200

                    return existingLevel;


                })
                .orElseGet(() -> {

                    // Niedodany
                    newAccessLevel.setActive(true);
                    newAccessLevel.setAccount(account);
                    accessLevelRepository.saveAndFlush(newAccessLevel);
                    accountRepository.saveAndFlush(account);

                    eventPublisher.publishEvent(
                            new AccountStatusChangedEvent(account.getId(), "addAccessLevel")
                    );
                    return newAccessLevel;
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager", readOnly = false)
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK12_REVOKE_ACCESS_LEVEL')")
    public void revokeAccessLevel(String login, String levelName) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(AccountNotFoundException::new);


        AccessLevel accessLevel = account.getAccessLevels().stream()
                .filter(al -> al.getLevelName().equals(levelName))
                .findFirst()
                .orElseThrow(AccessLevelDoesntExistsException::new);

        Principal principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal != null && login.equals(Objects.requireNonNull(principal).getName())
                && accessLevel instanceof Administrator) {
            throw new AdminCannotRevokeHisOwnAdminRole();
        }

        if (!accessLevel.isActive()) {
            return;
        }

        accessLevel.setActive(false);
        eventPublisher.publishEvent(
                new AccountStatusChangedEvent(account.getId(), "revokeAccessLevel")
        );

        accountRepository.saveAndFlush(account);
    }

    private void activateAccountByAdmin(Account account) {
        if (!account.isActive()) {
            account.setActive(true);
            accountRepository.saveAndFlush(account);

            eventPublisher.publishEvent(
                    new AccountStatusChangedEvent(account.getId(), "activated")
            );
        }
    }

    private void deactivateAccountByAdmin(Account account) {
        if (account.isActive()) {
            account.setActive(false);
            accountRepository.saveAndFlush(account);

            eventPublisher.publishEvent(
                    new AccountStatusChangedEvent(account.getId(), "deactivated")
            );
        }
    }

    //
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager", noRollbackFor = TokenExpiredException.class)
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    public void activateAccountWithToken(String token) {

        ActivationToken activationToken = activationTokenRepository.findByToken(token).orElseThrow(TokenNotFoundException::new);

        Account account = activationToken.getAccount();
        activationTokenRepository.delete(activationToken);
        if (!activationToken.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new TokenExpiredException();
        }
        activateAccountByAdmin(account);
    }


    // MOK.10 Odblokuj konto użytkownika
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK10_ACTIVATE_ACCOUNT')")
    public void activateAccountByAdmin(UUID id) {
        Account found = accountRepository.findById(id).orElseThrow(AccountNotFoundException::new);
        activateAccountByAdmin(found);
    }

    // MOK.9 Zablokuj konto użytkownika
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK9_DEACTIVATE_ACCOUNT')")
    public void deactivateAccountByAdmin(UUID id, String login) {
        Account found = accountRepository.findById(id).orElseThrow(AccountNotFoundException::new);
        if (found.getLogin().equals(login)) {
            throw new AdminCannotDeactivateOwnAccountException();
        }
        deactivateAccountByAdmin(found);
    }

    // S2_KODYJEDN — user przełącza 2FA na własnym koncie
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('SET_OWN_2FA')")
    public void setOwnTwoFa(String login, boolean enabled) {
        Account account = accountRepository.findByLogin(login).orElseThrow(AccountNotFoundException::new);
        account.setTwoFaEnabled(enabled);
        accountRepository.saveAndFlush(account);
    }


    //MOK.8
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    QueryTimeoutException.class
            },
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK8_DELETE_ACCOUNT')")
    public void deleteAccount(UUID id, String login) {
        Account found = accountRepository.findById(id).orElseThrow(AccountNotFoundException::new);
        if (found.getLogin().equals(login)) {
            throw new AdminCannotDeleteOwnAccountException();
        }
        found.getAccessLevels().forEach(e -> e.setActive(false));
        found.setDeleted(true);
        found.setActive(false);
        accountRepository.saveAndFlush(found);
        eventPublisher.publishEvent(new AccountStatusChangedEvent(id, "deleted"));
    }

    //MOK.7
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MOK7_ADD_USER')")
    @JaversCreate
    public Account addUser(Account account) {
        if (account.getAccessLevels() == null || account.getAccessLevels().isEmpty()) {
            throw new AccessLevelDoesntExistsException();
        }

        String normalizedEmail = account.getEmail().trim().toLowerCase();
        String normalizedLogin = account.getLogin().trim().toLowerCase();

        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setVerified(true);
        account.setActive(true);
        account.setRequiredNewPassword(true);
        account.setLogin(normalizedLogin);
        account.setEmail(normalizedEmail);
        account.setName(account.getName().trim());
        account.setSurname(account.getSurname().trim());


        // Access levels są kaskadowane (CascadeType.PERSIST), back-ref + active=true ustawia mapper
        return accountRepository.saveAndFlush(account);
    }



    private Account updateAccount(Account updatedAccount, Account targetAccount) {
        if (!targetAccount.getVersion().equals(updatedAccount.getVersion())) {
            throw new ConcurrentUpdateException();
        }

        //change updated fields in target account ( target Account -> old + updated fields)
        accountMapper.updateExistingAccount(updatedAccount, targetAccount);
        return accountRepository.saveAndFlush(targetAccount);
    }
    private Account updateAccount(String login, Account updatedAccount) {
        Account targetAccount = accountRepository.findByLogin(login).orElseThrow(AccountNotFoundException::new);
        return updateAccount(updatedAccount, targetAccount);
    }

    private Account updateAccount(UUID id, Account updatedAccount) {
        Account targetAccount = accountRepository.findById(id).orElseThrow(AccountNotFoundException::new);

        return updateAccount(updatedAccount, targetAccount);
    }

    //MOK.17
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MOK17_UPDATE_OWN_ACCOUNT')")
    public Account updateOwnAccount(String login, Account account) {
        return updateAccount(login, account);
    }


    //MOK.18
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MOK18_UPDATE_OTHER_ACCOUNT')")
    public Account updateOtherAccount(UUID id, Account account) {
        return  updateAccount(id, account);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MOK17_UPDATE_OWN_ACCOUNT')")
    public Account updatePreferencesOfOwnAccount(String login, Account updatedAccount) {
        Account targetAccount = accountRepository.findByLogin(login).orElseThrow(AccountNotFoundException::new);

        accountMapper.updateExistingAccount(updatedAccount, targetAccount);

        Account savedAccount = accountRepository.saveAndFlush(targetAccount);

        javers.commit(login,
                new AccountPreferencesAudit(
                        savedAccount.getLogin(),
                        savedAccount.getLanguage() != null ? savedAccount.getLanguage().name() : null,
                        savedAccount.getTheme() != null ? savedAccount.getTheme().name() : null
                )
        );

        return savedAccount;
    }
    private record AccountPreferencesAudit(String login,String language,String theme) {}


    //MOK.15 + MOK.16
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAnyAuthority('MOK15_CHANGE_OWN_EMAIL', 'MOK16_CHANGE_OTHER_EMAIL')")
    public ChangeEmailResponseDTO changeEmail(UUID id, String currentEmail, String newEmail) {

        Optional.of(currentEmail)
                .filter(email -> email.equalsIgnoreCase(newEmail))
                .ifPresent(email -> {
                    throw new EmailsAreTheSameException();
                });

        Optional.of(newEmail)
                .filter(accountRepository::existsByEmailIgnoreCase)
                .ifPresent(email -> {
                    throw new EmailIsAlreadyTakenException();
                });


        Account account = accountRepository.findById(id)
                .filter(acc -> acc.getEmail().equalsIgnoreCase(currentEmail))
                .orElseThrow(DetailsOfAccountDontMatchException::new);


        emailChangeTokenRepository.findByAccount(account)
                .ifPresent(token -> {
                    if (token.getExpiresAt().isAfter(Instant.now())) {
                        throw new AlreadyRequestedToChangeEmailException();
                    }

                    emailChangeTokenRepository.delete(token);
                    emailChangeTokenRepository.flush();
                });


        String token = UUID.randomUUID().toString();

        EmailChangeToken resetEmailToken = new EmailChangeToken();
        resetEmailToken.setToken(token);
        resetEmailToken.setOldEmail(currentEmail);
        resetEmailToken.setNewEmail(newEmail);
        resetEmailToken.setAccount(account);
        resetEmailToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));

        emailChangeTokenRepository.saveAndFlush(resetEmailToken);

        String link = frontendUrlBuilder.withToken("/auth/change-email", token);
        mailService.sendChangeNewEmailOfAccount(newEmail, link, Locale.forLanguageTag(account.getLanguage().toString()));

        String message = "Prośba o zmianę email została przyjęta! Sprawdź skrzynkę nowego e-mail, aby zatwierdzić zmiany.";
        return new ChangeEmailResponseDTO(message);

    }

    // MOK.14

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MOK14_CHANGE_USER_PASSWORD')")
    public void changeUserPassword(UUID targetId, UUID principalId, String newPassword) {
        if (targetId.equals(principalId)){
            throw new ItIsYourAccountException();
        }
        Account target = accountRepository.findById(targetId)
                .orElseThrow(AccountNotFoundException::new);

        target.setRequiredNewPassword(true);
        passwordUpdateService.changePasswordWithHistory(target, newPassword);
    }

    //MOK.13
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MOK13_CHANGE_OWN_PASSWORD')")
    public void changeOwnPassword(String login, String oldPassword, String newPassword) {

        Account account = accountRepository.findByLogin(login)
                .orElseThrow(AccountNotFoundException::new);

        if (!passwordEncoder.matches(oldPassword, account.getPassword())) {
            throw new InvalidOldPasswordException();
        }
        passwordUpdateService.changePasswordWithHistory(account, newPassword);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('CHANGE_VIEW_ACCESS_LEVEL')")
    public void changeViewAccessLevel(Collection<? extends GrantedAuthority> accessLevels, String requestedLevelName, String login) {
        boolean hasLevel = accessLevels.stream().anyMatch(a -> a.getAuthority().contains(requestedLevelName));
        if (hasLevel) {
            log.info("Change view access level '{}' to : {}", login, requestedLevelName);
        }

    }

    @Transactional(transactionManager = "mokTransactionManager", readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {CannotAcquireLockException.class, QueryTimeoutException.class},
            maxAttemptsExpression = "${app.transaction.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.transaction.retry.backoffMs}")
    )
    @PreAuthorize("hasAuthority('MOK4_GET_MY_ACCOUNT')")
    public PreferencesDataDTO getMyPreferences(UUID id) {
        PreferencesProjection projection = accountRepository.findPreferencesProjectedById(id).orElseThrow(AccountNotFoundException::new);
        return PreferencesDataDTO.builder().language(projection.getLanguage()).theme(projection.getTheme()).build();
    }

}
