package com.ape.friction.api.entities.users;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "client")
@DiscriminatorValue("CLIENT")
@ToString(callSuper = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Client extends AccessLevel implements Serializable {
    private static final long serialVersionUID = 1L;


}
