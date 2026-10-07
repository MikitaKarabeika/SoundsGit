package com.ape.friction.api.common;

import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.ape.friction.api.entities.users.Account;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractEntity {
    @Id
    @UUID7Generated
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @ToString.Exclude
    private Instant createdAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", updatable = false)
    private Account createdBy;

    @LastModifiedDate
    @Column(name = "modified_at", nullable = false)
    @ToString.Exclude
    private Instant modifiedAt;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    private Account lastModifiedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_operation_type", length = 20)
    private OperationType lastOperationType;


    @PrePersist
    protected void onPrePersist() {
        this.lastOperationType = OperationType.CREATE;
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.lastOperationType = OperationType.UPDATE;
    }

    @PreRemove
    protected void onPreRemove() {
        this.lastOperationType = OperationType.DELETE;
    }


}
