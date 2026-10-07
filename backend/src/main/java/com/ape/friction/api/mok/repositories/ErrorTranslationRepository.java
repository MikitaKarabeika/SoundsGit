package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.errorTranslation.ErrorTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;
import java.util.UUID;

@Repository
public interface ErrorTranslationRepository extends JpaRepository<ErrorTranslation, UUID> {
    Optional<ErrorTranslation> findByOriginalTextAndLanguageCode(String originalText, String languageCode);
}
