package com.ape.friction.api.entities.token;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class LoginCode extends AbstractEntity {

    @Basic(optional = false)
    @NotBlank
    @Size(max = 64)
    @Column(length = 64)
    @ToString.Exclude
    private String code;

    @OneToOne(optional = false)
    @NotNull
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @NotNull
    private LocalDateTime expiresDate;

    @NotNull
    @Column(name = "auth_session_id", unique = true, nullable = false)
    private UUID authSessionId;
}
