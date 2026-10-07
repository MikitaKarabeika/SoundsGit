package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.moderation.ModerationAction;
import com.ape.friction.api.entities.moderation.ModerationActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    List<ModerationAction> findByAccount_IdInOrderByCreatedAtDescIdDesc(Collection<UUID> accountIds);

    Page<ModerationAction> findByAccount_IdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);

    Optional<ModerationAction> findFirstByAccount_IdAndActionTypeInOrderByCreatedAtDescIdDesc(
            UUID accountId,
            Collection<ModerationActionType> actionTypes
    );

    Optional<ModerationAction> findFirstByReport_Id(UUID reportId);
}
