package com.ape.friction.integration.config;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class MailConfiguration {
    @Bean
    @ServiceConnection
    public MailpitContainer mailpitContainer() {
        return new MailpitContainer();
    }
}
