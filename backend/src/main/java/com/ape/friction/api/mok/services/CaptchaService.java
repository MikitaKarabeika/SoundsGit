package com.ape.friction.api.mok.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ape.friction.api.exceptions.CaptchaVerificationException;
import com.ape.friction.api.interceptors.MethodCallLogged;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@MethodCallLogged
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.captcha.verify-url:http://nginx/recaptcha-proxy/api/siteverify}")
    private String verifyUrl;

    @Value("${app.captcha.secret:${RECAPTCHA_SECRET:test-recaptcha-secret}}")
    private String secret;

    @Value("${app.captcha.timeout-seconds:8}")
    private long timeoutSeconds;

    @Value("${app.captcha.enabled:true}")
    private boolean enabled;

    public void verify(String token) {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new CaptchaVerificationException();
        }

        try {
            String form = "secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder(URI.create(verifyUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Captcha provider returned status {}", response.statusCode());
                throw new CaptchaVerificationException();
            }

            JsonNode body = objectMapper.readTree(response.body());
            JsonNode success = body.get("success");
            if (success == null || !success.asBoolean(false)) {
                log.warn("Captcha verification rejected by provider: {}", response.body());
                throw new CaptchaVerificationException();
            }
        } catch (CaptchaVerificationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Captcha verification call failed", e);
            throw new CaptchaVerificationException();
        }
    }
}
