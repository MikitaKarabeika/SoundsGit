package com.ape.friction.api.entities.token;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;

import java.time.LocalDateTime;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class ActivationToken extends AbstractEntity {
    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(optional = false)
    @NotNull
    private Account account;

    @NotNull
    private LocalDateTime expiresAt;
}

