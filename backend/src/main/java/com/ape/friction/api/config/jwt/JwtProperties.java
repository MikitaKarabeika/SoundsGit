package com.ape.friction.api.config.jwt;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private SecretKey secret;
    private Expiration expiration;
    private Session session = new Session();

    @Getter
    @Setter
    public static class Session {
        private long warningThresholdSeconds = 60;
    }

    @Getter
    @Setter
    public static class Expiration {
        private long access;
        private long refresh;
    }

    public void setSecret(String secret) {
        this.secret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); //Podobno spring sobie poradzi hehe
    }
}
