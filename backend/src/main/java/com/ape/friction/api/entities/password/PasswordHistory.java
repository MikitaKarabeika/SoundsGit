package com.ape.friction.api.entities.password;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.entities.users.Account;

@Entity
@Table(name = "password_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHistory extends AbstractEntity {

    @Basic(optional = false)
    @NotBlank
    @Size(max = 64)
    @Column(name = "password_hash", length = 64, updatable = false)
    @ToString.Exclude
    private String password;

    @ManyToOne(optional = false , fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;


}
