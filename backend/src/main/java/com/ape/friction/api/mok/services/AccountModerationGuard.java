package com.ape.friction.api.mok.services;

import com.ape.friction.api.entities.moderation.ModerationAction;
import com.ape.friction.api.entities.moderation.ModerationActionType;
import com.ape.friction.api.mok.repositories.ModerationActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountModerationGuard {

    private static final EnumSet<ModerationActionType> ENFORCEMENT_ACTIONS =
            EnumSet.of(ModerationActionType.TIMEOUT, ModerationActionType.PERMANENT_BAN, ModerationActionType.LIFT);

    private final ModerationActionRepository actionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    public boolean isRestricted(UUID accountId) {
        Optional<ModerationAction> latest = latestEnforcementAction(accountId);
        if (latest.isEmpty()) {
            return false;
        }

        ModerationAction action = latest.get();
        return action.getActionType() == ModerationActionType.PERMANENT_BAN
                || (action.getActionType() == ModerationActionType.TIMEOUT
                && action.getExpiresAt() != null
                && action.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
    public boolean isPermanentlyBanned(UUID accountId) {
        return latestEnforcementAction(accountId)
                .map(action -> action.getActionType() == ModerationActionType.PERMANENT_BAN)
                .orElse(false);
    }

    private Optional<ModerationAction> latestEnforcementAction(UUID accountId) {
        return actionRepository.findFirstByAccount_IdAndActionTypeInOrderByCreatedAtDescIdDesc(
                accountId,
                ENFORCEMENT_ACTIONS
        );
    }
}
