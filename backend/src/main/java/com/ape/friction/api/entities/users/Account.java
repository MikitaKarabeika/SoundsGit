package com.ape.friction.api.entities.users;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;
import com.ape.friction.api.common.AbstractEntity;
import com.ape.friction.api.common.Language;
import com.ape.friction.api.entities.password.PasswordHistory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Entity
@NoArgsConstructor
@Table(name = "account")
@SecondaryTable(name="personal_data")
@SQLRestriction("deleted <> 'true'")
@Getter @Setter
public class Account extends AbstractEntity {

    @Basic(optional = false)
    @NotBlank
    @Size(min = 4, max = 64)
    @Column(updatable = false, length = 64, unique=true)
    private String login;

    @Basic(optional = false)
    @NotBlank
    @Size(/*min = 64,*/ max = 64)
    @Column(length = 64)
    @ToString.Exclude
    private String password;

    @Basic(optional = false)
    private boolean verified = false;

    @Basic(optional = false)
    private boolean active = true;

    @Basic(optional = false)
    @Column(name = "two_fa_enabled", nullable = false)
    private boolean twoFaEnabled = false;


    @Basic(optional = false)
    @Column(name = "required_new_password", nullable = false)
    private boolean requiredNewPassword = false;

    @Basic(optional = false)
    @Column(nullable = false)
    private boolean deleted = false;

    // Bedzie trzeba tez do personal_data
    @Enumerated(EnumType.STRING)
    @Basic(optional = false)
    private Language language = Language.EN;

    @Enumerated(EnumType.STRING)
    @Basic(optional = false)
    @Column(table = "personal_data", name = "theme")
    private Theme theme = Theme.LIGHT;

    @Basic(optional = false)
    @NotBlank
    @Size(min = 1, max = 32)
    @Column(table="personal_data", length = 32)
    private String name;

    @Basic(optional = false)
    @Column(name="failed_attempts")
    private int failedAttempts = 0;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Basic(optional = false)
    @NotBlank
    @Size(min = 1, max = 32)
    @Column(table="personal_data", length = 32)
    private String surname;

    @NotBlank
    @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Size(max = 128)
    @Column(table="personal_data", length = 128, unique = true)
    private String email;


    @ToString.Exclude
    @Getter
    @OneToMany(mappedBy = "account", fetch = FetchType.EAGER,cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @BatchSize(size=50)
    private Collection<AccessLevel> accessLevels = new ArrayList<>();

    @Column(name = "timezone", nullable = false)
    private String timezone = "UTC";

    @Column(name = "last_login_at")
    private Instant lastLoginAt;


    @OneToMany(
            mappedBy = "account",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @ToString.Exclude
    @OrderBy("createdAt DESC")
    private List<PasswordHistory> passwordHistory = new ArrayList<>();

    public void addPasswordToHistory(String oldPasswordHash) {
        PasswordHistory history = new PasswordHistory(oldPasswordHash, this);
        this.passwordHistory.add(history);
    }


}
