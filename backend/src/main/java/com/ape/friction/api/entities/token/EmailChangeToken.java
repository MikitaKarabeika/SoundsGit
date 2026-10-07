package com.ape.friction.api.entities.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class EmailChangeToken extends AbstractEntity {

    @Column(nullable = false, unique = true)
    @NotBlank
    private String token;

    @Column(nullable = false)
    @NotBlank
    private String newEmail;

    @Column(nullable = false)
    @NotBlank
    private String oldEmail;


    @Column(nullable = false)
    @NotNull
    private Instant expiresAt;

    @OneToOne(optional = false)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull
    private Account account;




}
