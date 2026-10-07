package com.ape.friction.api.mok.services;

import com.ape.friction.api.dto.request.CreateModerationReportRequestDTO;
import com.ape.friction.api.dto.request.ModerationActionRequestDTO;
import com.ape.friction.api.dto.request.ModerationDecisionRequestDTO;
import com.ape.friction.api.dto.response.AccountModerationStatusDTO;
import com.ape.friction.api.dto.response.ModerationActionResponseDTO;
import com.ape.friction.api.dto.response.ModerationDecisionResponseDTO;
import com.ape.friction.api.dto.response.ModerationReportResponseDTO;
import com.ape.friction.api.entities.moderation.ModerationAction;
import com.ape.friction.api.entities.moderation.ModerationActionType;
import com.ape.friction.api.entities.moderation.ModerationAccountStatus;
import com.ape.friction.api.entities.moderation.ModerationDecision;
import com.ape.friction.api.entities.moderation.ModerationReport;
import com.ape.friction.api.entities.moderation.ModerationSanction;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.entities.users.Administrator;
import com.ape.friction.api.exceptions.AccountNotFoundException;
import com.ape.friction.api.exceptions.moderation.AccountAlreadyPermanentlyBannedException;
import com.ape.friction.api.exceptions.moderation.AccountAlreadyRestrictedException;
import com.ape.friction.api.exceptions.moderation.AccountPermanentlyBannedException;
import com.ape.friction.api.exceptions.moderation.CannotBanOwnAccountException;
import com.ape.friction.api.exceptions.moderation.CannotReportOwnAccountException;
import com.ape.friction.api.exceptions.moderation.CannotSanctionOwnAccountException;
import com.ape.friction.api.exceptions.moderation.DismissedReportCannotHaveSanctionException;
import com.ape.friction.api.exceptions.moderation.InvalidModerationTimeoutException;
import com.ape.friction.api.exceptions.moderation.ModerationDurationOnlyForTimeoutException;
import com.ape.friction.api.exceptions.moderation.ModerationReportAlreadyDecidedException;
import com.ape.friction.api.exceptions.moderation.ModerationReportNotFoundException;
import com.ape.friction.api.exceptions.moderation.ModeratorCannotSanctionAdministratorException;
import com.ape.friction.api.exceptions.moderation.NoActiveModerationRestrictionException;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.AccountRepository;
import com.ape.friction.api.mok.repositories.ModerationActionRepository;
import com.ape.friction.api.mok.repositories.ModerationDecisionRepository;
import com.ape.friction.api.mok.repositories.ModerationReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@MethodCallLogged
@TransactionLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
public class ModerationService {

    private final AccountRepository accountRepository;
    private final ModerationReportRepository reportRepository;
    private final ModerationDecisionRepository decisionRepository;
    private final ModerationActionRepository actionRepository;
    private final AccountModerationGuard moderationGuard;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MODERATION_CREATE_REPORT')")
    public ModerationReportResponseDTO createReport(UUID reporterId, CreateModerationReportRequestDTO request) {
        if (reporterId.equals(request.reportedAccountId())) {
            throw new CannotReportOwnAccountException();
        }

        Account reporter = accountRepository.findById(reporterId).orElseThrow(AccountNotFoundException::new);
        Account reported = accountRepository.findById(request.reportedAccountId())
                .orElseThrow(AccountNotFoundException::new);

        ModerationReport report = reportRepository.saveAndFlush(
                new ModerationReport(reporter, reported, request.category(), request.details().trim())
        );
        return toReportResponse(report);
    }

    @PreAuthorize("hasAuthority('MODERATION_REVIEW_REPORTS')")
    public Page<ModerationReportResponseDTO> getOpenReports(Pageable pageable) {
        return reportRepository.findOpenReports(pageable).map(this::toReportResponse);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MODERATION_REVIEW_REPORTS')")
    public ModerationDecisionResponseDTO decideReport(
            UUID reportId,
            UUID moderatorId,
            ModerationDecisionRequestDTO request
    ) {
        ModerationReport report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(ModerationReportNotFoundException::new);

        if (decisionRepository.existsByReport_Id(reportId)) {
            throw new ModerationReportAlreadyDecidedException();
        }

        validateSanctionRequest(request);
        Account moderator = accountRepository.findById(moderatorId).orElseThrow(AccountNotFoundException::new);
        ModerationDecision decision = decisionRepository.saveAndFlush(
                new ModerationDecision(report, moderator, request.outcome(), request.note().trim())
        );
        if (request.outcome() == com.ape.friction.api.entities.moderation.ModerationOutcome.CONFIRMED) {
            Account target = report.getReportedAccount();
            if (moderatorId.equals(target.getId())) {
                throw new CannotSanctionOwnAccountException();
            }
            ModerationSanction sanction = request.sanction() == null
                    ? ModerationSanction.WARNING
                    : request.sanction();
            if (isAdministrator(target)) {
                throw new ModeratorCannotSanctionAdministratorException();
            }
            if (moderationGuard.isPermanentlyBanned(target.getId())) {
                throw new AccountPermanentlyBannedException();
            }
            if (sanction == ModerationSanction.TIMEOUT && moderationGuard.isRestricted(target.getId())) {
                throw new AccountAlreadyRestrictedException();
            }

            ModerationActionType actionType = sanction == ModerationSanction.WARNING
                    ? ModerationActionType.WARNING
                    : ModerationActionType.TIMEOUT;
            Instant expiresAt = actionType == ModerationActionType.TIMEOUT
                    ? Instant.now().plus(request.durationHours(), ChronoUnit.HOURS)
                    : null;
            actionRepository.saveAndFlush(new ModerationAction(
                    target, moderator, report, actionType, request.note().trim(), expiresAt
            ));
        }
        return toDecisionResponse(decision);
    }

    @PreAuthorize("hasAuthority('MODERATION_VIEW_DECISIONS')")
    public Page<ModerationDecisionResponseDTO> getDecisionLog(Pageable pageable) {
        return decisionRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDecisionResponse);
    }

    @PreAuthorize("hasAuthority('MODERATION_ADMIN_ACCOUNTS')")
    public Page<AccountModerationStatusDTO> getAccountStatuses(Pageable pageable) {
        Page<Account> accounts = accountRepository.findAll(pageable);
        List<UUID> accountIds = accounts.getContent().stream().map(Account::getId).toList();
        Map<UUID, List<ModerationAction>> actionsByAccount = new HashMap<>();
        if (!accountIds.isEmpty()) {
            actionRepository.findByAccount_IdInOrderByCreatedAtDescIdDesc(accountIds)
                    .forEach(action -> actionsByAccount
                            .computeIfAbsent(action.getAccount().getId(), ignored -> new ArrayList<>())
                            .add(action));
        }

        Instant now = Instant.now();
        return accounts.map(account -> toAccountStatus(
                account,
                actionsByAccount.getOrDefault(account.getId(), List.of()),
                now
        ));
    }

    @PreAuthorize("hasAuthority('MODERATION_ADMIN_ACCOUNTS')")
    public AccountModerationStatusDTO getAccountStatus(UUID accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        List<ModerationAction> actions = actionRepository
                .findByAccount_IdInOrderByCreatedAtDescIdDesc(List.of(accountId));
        return toAccountStatus(account, actions, Instant.now());
    }

    @PreAuthorize("hasAuthority('MODERATION_ADMIN_ACCOUNTS')")
    public Page<ModerationActionResponseDTO> getAccountActionHistory(UUID accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException();
        }
        return actionRepository.findByAccount_IdOrderByCreatedAtDescIdDesc(accountId, pageable)
                .map(this::toActionResponse);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MODERATION_ADMIN_ACCOUNTS')")
    public AccountModerationStatusDTO permanentlyBan(
            UUID accountId,
            UUID adminId,
            ModerationActionRequestDTO request
    ) {
        if (accountId.equals(adminId)) {
            throw new CannotBanOwnAccountException();
        }
        Account target = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        Account admin = accountRepository.findById(adminId).orElseThrow(AccountNotFoundException::new);
        if (moderationGuard.isPermanentlyBanned(accountId)) {
            throw new AccountAlreadyPermanentlyBannedException();
        }
        ModerationAction action = actionRepository.saveAndFlush(new ModerationAction(
                target, admin, null, ModerationActionType.PERMANENT_BAN, request.note().trim(), null
        ));
        return toAccountStatus(target, List.of(action), Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false, transactionManager = "mokTransactionManager")
    @PreAuthorize("hasAuthority('MODERATION_ADMIN_ACCOUNTS')")
    public AccountModerationStatusDTO liftSanction(
            UUID accountId,
            UUID adminId,
            ModerationActionRequestDTO request
    ) {
        Account target = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        if (!moderationGuard.isRestricted(accountId)) {
            throw new NoActiveModerationRestrictionException();
        }
        Account admin = accountRepository.findById(adminId).orElseThrow(AccountNotFoundException::new);
        ModerationAction action = actionRepository.saveAndFlush(new ModerationAction(
                target, admin, null, ModerationActionType.LIFT, request.note().trim(), null
        ));
        return toAccountStatus(target, List.of(action), Instant.now());
    }

    private ModerationReportResponseDTO toReportResponse(ModerationReport report) {
        return new ModerationReportResponseDTO(
                report.getId(),
                report.getReporter().getId(),
                report.getReporterLogin(),
                report.getReportedAccount().getId(),
                report.getReportedLogin(),
                report.getCategory(),
                report.getDetails(),
                report.getCreatedAt()
        );
    }

    private ModerationDecisionResponseDTO toDecisionResponse(ModerationDecision decision) {
        ModerationAction action = actionRepository.findFirstByReport_Id(decision.getReport().getId()).orElse(null);
        ModerationSanction sanction = action == null ? null : switch (action.getActionType()) {
            case WARNING -> ModerationSanction.WARNING;
            case TIMEOUT -> ModerationSanction.TIMEOUT;
            case PERMANENT_BAN, LIFT -> null;
        };
        return new ModerationDecisionResponseDTO(
                decision.getId(),
                decision.getReport().getId(),
                decision.getReport().getReportedLogin(),
                decision.getReport().getReporterLogin(),
                decision.getModerator().getLogin(),
                decision.getOutcome(),
                decision.getNote(),
                decision.getCreatedAt(),
                sanction,
                action == null ? null : action.getExpiresAt()
        );
    }

    private void validateSanctionRequest(ModerationDecisionRequestDTO request) {
        if (request.outcome() == com.ape.friction.api.entities.moderation.ModerationOutcome.DISMISSED
                && (request.sanction() != null || request.durationHours() != null)) {
            throw new DismissedReportCannotHaveSanctionException();
        }
        if (request.sanction() == ModerationSanction.TIMEOUT
                && (request.durationHours() == null || request.durationHours() < 1 || request.durationHours() > 168)) {
            throw new InvalidModerationTimeoutException();
        }
        if (request.sanction() != ModerationSanction.TIMEOUT && request.durationHours() != null) {
            throw new ModerationDurationOnlyForTimeoutException();
        }
    }

    private boolean isAdministrator(Account account) {
        return account.getAccessLevels().stream()
                .anyMatch(level -> level.isActive() && level instanceof Administrator);
    }

    private AccountModerationStatusDTO toAccountStatus(
            Account account,
            List<ModerationAction> actions,
            Instant now
    ) {
        ModerationAction latest = actions.isEmpty() ? null : actions.get(0);
        ModerationAction enforcement = actions.stream()
                .filter(action -> EnumSet.of(
                        ModerationActionType.TIMEOUT,
                        ModerationActionType.PERMANENT_BAN,
                        ModerationActionType.LIFT
                ).contains(action.getActionType()))
                .findFirst()
                .orElse(null);

        ModerationAccountStatus status = ModerationAccountStatus.ACTIVE;
        ModerationAction displayedAction = latest;
        if (enforcement != null && enforcement.getActionType() == ModerationActionType.PERMANENT_BAN) {
            status = ModerationAccountStatus.BANNED;
            displayedAction = enforcement;
        } else if (enforcement != null && enforcement.getActionType() == ModerationActionType.TIMEOUT
                && enforcement.getExpiresAt() != null && enforcement.getExpiresAt().isAfter(now)) {
            status = ModerationAccountStatus.TIMED_OUT;
            displayedAction = enforcement;
        } else if (latest != null && latest.getActionType() == ModerationActionType.WARNING) {
            status = ModerationAccountStatus.WARNED;
        }

        return new AccountModerationStatusDTO(
                account.getId(),
                account.getLogin(),
                status,
                displayedAction == null ? null : displayedAction.getNote(),
                displayedAction == null ? null : displayedAction.getActor().getLogin(),
                displayedAction == null ? null : displayedAction.getCreatedAt(),
                displayedAction == null ? null : displayedAction.getExpiresAt(),
                displayedAction == null || displayedAction.getReport() == null
                        ? null : displayedAction.getReport().getId()
        );
    }

    private ModerationActionResponseDTO toActionResponse(ModerationAction action) {
        return new ModerationActionResponseDTO(
                action.getId(),
                action.getActionType(),
                action.getNote(),
                action.getActor().getLogin(),
                action.getCreatedAt(),
                action.getExpiresAt(),
                action.getReport() == null ? null : action.getReport().getId()
        );
    }
}
