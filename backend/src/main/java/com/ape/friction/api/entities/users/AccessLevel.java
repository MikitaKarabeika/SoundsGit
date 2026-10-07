package com.ape.friction.api.entities.users;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.ape.friction.api.common.AbstractEntity;

import java.io.Serializable;

@Entity
@Table(name = "access_level")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "level")
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AccessLevel extends AbstractEntity implements Serializable {


    private static final long serialVersionUID = 1L;


    @Basic(optional = false)
    @Column(name = "level", updatable = false, insertable=false)
    private String levelName;

    @Basic(optional = false)
    @Setter
    private boolean active = true;


    @JoinColumn(updatable = false)
    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @Setter
    private Account account;




    //Metoda pomocnicza do tworzenia ról
    public String getAccessLevels() {
        if (this.active) {
            return "ROLE_" + levelName;
        }
        else{
            return "";
        }

    }
}
