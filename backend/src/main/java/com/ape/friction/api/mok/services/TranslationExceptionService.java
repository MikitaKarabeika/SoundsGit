package com.ape.friction.api.mok.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.ape.friction.api.entities.errorTranslation.ErrorTranslation;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;
import com.ape.friction.api.mok.repositories.ErrorTranslationRepository;

import java.net.URI;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager", readOnly = true)
@MethodCallLogged
@TransactionLogged
public class TranslationExceptionService {


    private final ErrorTranslationRepository repository;

    @Value("${app.translation.mymemory.base-url:http://nginx/mymemory-proxy/}")
    private String baseUrl;

    private final RestTemplateBuilder restTemplateBuilder;

    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "mokTransactionManager", readOnly = false)
    public String translateErrorText(String englishText, String targetLang) {
        String lang = targetLang.split("[-_]")[0].split(",")[0].toLowerCase();

        if ("en".equals(lang)) {

            return englishText;
        }
        Optional<ErrorTranslation> cached = repository.findByOriginalTextAndLanguageCode(englishText, lang);
        if (cached.isPresent()) {

            return cached.get().getTranslatedText();
        }

        String translated;
        try {

            String langpair = "en|" + lang;

            URI uri = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .queryParam("q", englishText)
                    .queryParam("langpair", langpair)
                    .build()
                    .encode()
                    .toUri();

            RestTemplate restTemplate = restTemplateBuilder.build();
            MyMemoryResponse response = restTemplate.getForObject(uri, MyMemoryResponse.class);
            if (response != null && response.getResponseData() != null) {

                translated = response.getResponseData().getTranslatedText();
            } else {

                return englishText;
            }
        } catch (Exception e) {
                return englishText;
        }

        ErrorTranslation newTranslation = new ErrorTranslation(englishText, lang, translated);
        repository.saveAndFlush(newTranslation);

        return translated;
    }

    @Setter
    @Getter
    public static class MyMemoryResponse {
        private ResponseData responseData;

    }
    @Setter
    @Getter
    public static class ResponseData {
        private String translatedText;

    }
}
