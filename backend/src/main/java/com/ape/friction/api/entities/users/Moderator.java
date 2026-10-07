package com.ape.friction.api.entities.users;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Entity
@Table(name = "moderator")
@DiscriminatorValue("MODERATOR")
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Moderator extends AccessLevel implements Serializable {
    private static final long serialVersionUID = 1L;
}
