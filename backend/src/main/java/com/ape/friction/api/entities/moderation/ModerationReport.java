package com.ape.friction.api.entities.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;

@Entity
@Table(name = "moderation_report")
@Getter
@NoArgsConstructor
public class ModerationReport extends AbstractEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_account_id", nullable = false)
    private Account reporter;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_account_id", nullable = false)
    private Account reportedAccount;

    @Column(name = "reporter_login", nullable = false, length = 64)
    private String reporterLogin;

    @Column(name = "reported_login", nullable = false, length = 64)
    private String reportedLogin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportCategory category;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String details;

    public ModerationReport(Account reporter, Account reportedAccount, ReportCategory category, String details) {
        this.reporter = reporter;
        this.reportedAccount = reportedAccount;
        this.reporterLogin = reporter.getLogin();
        this.reportedLogin = reportedAccount.getLogin();
        this.category = category;
        this.details = details;
    }
}
