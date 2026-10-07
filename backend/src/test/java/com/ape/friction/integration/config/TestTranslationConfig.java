package com.ape.friction.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import  com.ape.friction.api.mok.services.TranslationExceptionService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestTranslationConfig {
    @Bean
    @Primary
    public TranslationExceptionService mockTranslationService() {
        TranslationExceptionService mock = mock(TranslationExceptionService.class);
        when(mock.translateErrorText(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return mock;
    }
}
