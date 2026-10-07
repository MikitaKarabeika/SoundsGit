package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.moderation.ModerationDecision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface ModerationDecisionRepository extends JpaRepository<ModerationDecision, UUID> {

    Page<ModerationDecision> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByReport_Id(UUID reportId);
}
