package com.ape.friction.api.mok.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ape.friction.api.config.jwt.JwtProperties;
import com.ape.friction.api.entities.users.AccessLevel;
import com.ape.friction.api.entities.users.Account;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.SessionLogged;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@MethodCallLogged
public class JwtService {

    private final JwtProperties jwtProperties;

    public Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(jwtProperties.getSecret()).build().parseSignedClaims(token).getPayload());

        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }


    }

    //Brak domyślnych parametrów w java
    //Rozwiązanie dla CODELOGIN, gdy potrzebny jest Token z krótszą datą ważności
    private String generateAccessToken(Account account) {
       return generateAccessToken(account,1);
    }


    private String generateRefreshToken(Account account) {
        return generateRefreshToken(account,1);
    }


    private String generateAccessToken(Account account, int tokenExpirationFactor) {
        List<String> accessLevels = account.getAccessLevels()
                .stream()
                .map(AccessLevel::getAccessLevels)
                .filter((v) -> !v.isEmpty())
                .toList();

        return Jwts.builder().setSubject(account.getId().toString())
                .claim("login", account.getLogin())
                .claim("accessLevels", accessLevels)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration().getAccess()/tokenExpirationFactor))
                .signWith(jwtProperties.getSecret()).compact();
    }


    private String generateRefreshToken(Account account, int tokenExpirationFactor) {
        return Jwts.builder()
                .setSubject(account.getId().toString())
                .claim("type", "refresh")
                .claim("login", account.getLogin())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration().getRefresh()/tokenExpirationFactor))
                .signWith(jwtProperties.getSecret())
                .compact();
    }


    public List<String> refresh(Account account) {
        return Arrays.asList(
                generateAccessToken(account),
                generateRefreshToken(account)
        );
    }


    @SessionLogged
    public List<String> authenticate(Account account,int tokenExpirationFactor) {
        return Arrays.asList(
                generateAccessToken(account,tokenExpirationFactor),
                generateRefreshToken(account,tokenExpirationFactor)
        );
    }

    @SessionLogged
    public List<String> authenticate(Account account) {
        return Arrays.asList(
                generateAccessToken(account),
                generateRefreshToken(account)
        );
    }

}
