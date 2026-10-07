package com.ape.friction.api.common;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class AbstractTranslationEntity<T> extends AbstractEntity {
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private T parent;
}