package com.ape.friction.api.entities.users;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Entity
@Table(name = "admin")
@Data
@DiscriminatorValue("ADMIN")
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Administrator extends AccessLevel implements Serializable {
    private static final long serialVersionUID = 1L;



}
