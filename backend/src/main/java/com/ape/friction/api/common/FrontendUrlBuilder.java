package com.ape.friction.api.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FrontendUrlBuilder {

    private final String baseUrl;

    public FrontendUrlBuilder(@Value("${app.frontend.url}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public String withToken(String path, String token) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
