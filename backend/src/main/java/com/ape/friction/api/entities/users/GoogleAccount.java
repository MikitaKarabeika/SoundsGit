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

@Entity
@Table(name = "google_account")
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class GoogleAccount extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @ToString.Exclude
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @Column(name = "google_id", nullable = false, unique = true, updatable = false)
    @NotNull
    private String googleId;
}
