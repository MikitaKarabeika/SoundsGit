package com.ape.friction.api.entities.opinion;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.ape.friction.api.common.AbstractEntity;
//import com.ape.friction.api.entities.meal.Meal;
import com.ape.friction.api.entities.users.Client;

@Getter
@Setter
@Table(name = "opinion", uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "meal_id"})
)
@NoArgsConstructor
@Entity
public class Opinion extends AbstractEntity {

    @NotNull
    @Column(name = "score")
    @Min(1)
    @Max(5)
    private Integer score;

    @Column(name = "comment")
    private String comment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

//    @NotNull
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "meal_id", nullable = false)
//    private Meal meal;

}
