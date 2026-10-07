package com.ape.friction.api.mappers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "app.role-mapping")
public class RoleMapper {

    private Map<String, List<String>> mapping = new HashMap<>();

    public List<GrantedAuthority> mapAccessLevels(List<String> accessLevels) {
        if (accessLevels == null) return List.of();
        return accessLevels.stream()
                .map(level -> level.startsWith("ROLE_") ? level.substring(5) : level)
                .flatMap(level -> mapping.getOrDefault(level, List.of()).stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }


    public Map<String, List<String>> getMapping() { return mapping; }
    public void setMapping(Map<String, List<String>> mapping) { this.mapping = mapping; }
}
