package com.ape.friction.api.entities.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;

import java.time.Instant;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor

public class VerificationToken extends AbstractEntity {

    @Column(nullable = false, unique = true)
    @NotBlank
    private String token;

    @OneToOne(optional = false)
    @NotNull
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @NotNull
    private Instant expiresDate;

    @Column(nullable = false)
    private boolean reminderSent = false;
}
