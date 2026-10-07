package com.ape.friction.api.entities.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.ape.friction.api.common.AbstractEntity;

import java.time.Instant;

@Entity
@Table(name = "login_audit")
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class LoginAudit extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @ToString.Exclude
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_successful_auth_ip", length = 45)
    private String lastSuccessfulAuthIp;

    @Column(name = "last_failed_auth_time")
    private Instant lastFailedAuthTime;

    @Column(name = "last_failed_auth_ip", length = 45)
    private String lastFailedAuthIp;

    public LoginAudit(Account account) {
        this.account = account;
    }
}
