package com.ape.friction.api.entities.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;

@Entity
@Table(name = "moderation_decision")
@Getter
@NoArgsConstructor
public class ModerationDecision extends AbstractEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private ModerationReport report;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_account_id", nullable = false)
    private Account moderator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModerationOutcome outcome;

    @Column(nullable = false, length = 1000)
    private String note;

    public ModerationDecision(
            ModerationReport report,
            Account moderator,
            ModerationOutcome outcome,
            String note
    ) {
        this.report = report;
        this.moderator = moderator;
        this.outcome = outcome;
        this.note = note;
    }
}
