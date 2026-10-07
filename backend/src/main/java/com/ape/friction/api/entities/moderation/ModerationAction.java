package com.ape.friction.api.entities.moderation;

import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "moderation_action")
@Getter
@NoArgsConstructor
public class ModerationAction extends AbstractEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_account_id", nullable = false)
    private Account actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private ModerationReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 24)
    private ModerationActionType actionType;

    @Column(nullable = false, length = 1000)
    private String note;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public ModerationAction(
            Account account,
            Account actor,
            ModerationReport report,
            ModerationActionType actionType,
            String note,
            Instant expiresAt
    ) {
        this.account = account;
        this.actor = actor;
        this.report = report;
        this.actionType = actionType;
        this.note = note;
        this.expiresAt = expiresAt;
    }
}
