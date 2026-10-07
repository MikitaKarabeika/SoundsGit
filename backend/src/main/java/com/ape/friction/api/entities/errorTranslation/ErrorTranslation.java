package com.ape.friction.api.entities.errorTranslation;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.ape.friction.api.common.AbstractEntity;

@Entity
@Table(
        name = "error_translation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"original_text", "language_code"})
        },
        indexes = {
                @Index(name = "idx_original_lang", columnList = "original_text, language_code")}

)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorTranslation extends AbstractEntity {

    @Column(name = "original_text", nullable = false)
    private String originalText;

    @Column(name = "language_code", nullable = false, length = 2)
    private String languageCode;

    @Column(name = "translated_text", nullable = false)
    private String translatedText;

}
