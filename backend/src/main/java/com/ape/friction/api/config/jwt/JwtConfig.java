package com.ape.friction.api.config.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class) //potrzebne do działania JwtProperties
public class JwtConfig {

}
