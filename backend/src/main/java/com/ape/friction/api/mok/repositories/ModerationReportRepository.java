package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.moderation.ModerationReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface ModerationReportRepository extends JpaRepository<ModerationReport, UUID> {

    @Query("""
            select report
            from ModerationReport report
            where not exists (
                select decision.id
                from ModerationDecision decision
                where decision.report = report
            )
            """)
    Page<ModerationReport> findOpenReports(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from ModerationReport report where report.id = :id")
    Optional<ModerationReport> findByIdForUpdate(@Param("id") UUID id);
}
