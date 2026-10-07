package com.ape.friction.api.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import com.ape.friction.api.dto.signature.Signable;
import com.ape.friction.api.exceptions.ConcurrentUpdateException;
import com.ape.friction.api.exceptions.signature.InvalidSignatureException;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.SignedData;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
@NoArgsConstructor
@MethodCallLogged
@ConfigurationProperties(prefix = "jws")
public class JwsService {

    private SecretKey secret;

    public void setSecret(@Value("${jws.secret}") String secret) {
        this.secret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Signable object) {
        return generate(object.getSignableProperties());
    }

    public String generate(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .signWith(secret)
                .compact();
    }

    public SignedData validate(String jws) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(jws)
                    .getPayload();

            return new SignedData(
                    UUID.fromString(claims.get("id", String.class)),
                    claims.get("version", Integer.class).longValue(),
                    claims
            );
        } catch (Exception e) {
            throw new InvalidSignatureException();
        }
    }

    public void verifyMap(String jwsToken, Map<String, Object> expected) {
        Map<String, Object> signedClaims = validate(jwsToken).allClaims();

        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            String key = entry.getKey();
            Object actual = signedClaims.get(key);
            if (actual == null) {
                throw new InvalidSignatureException();
            }
            String expectedStr = convertValueToString(entry.getValue());
            String actualStr   = convertValueToString(actual);
            if (!expectedStr.equals(actualStr)) {
                if ("version".equals(key)) throw new ConcurrentUpdateException();
                throw new InvalidSignatureException();
            }
        }
    }

    private String convertValueToString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString())
                    .stripTrailingZeros()
                    .toPlainString();
        }
        return value.toString();
    }
}
