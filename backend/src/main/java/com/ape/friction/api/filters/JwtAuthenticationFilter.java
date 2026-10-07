package com.ape.friction.api.filters;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ape.friction.api.filters.userPrincipal.UserPrincipal;
import com.ape.friction.api.mappers.RoleMapper;
import com.ape.friction.api.mok.services.AccountModerationGuard;
import com.ape.friction.api.mok.services.JwtService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Order(999) //UsernamePasswordAuthenticationFilter domyślnie ma 1000
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RoleMapper roleMapper;
    private final AccountModerationGuard accountModerationGuard;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Optional<Claims> claims = extractToken(request)
                .flatMap(jwtService::parseClaims);

        if (claims.isEmpty() || "refresh".equals(claims.get().get("type", String.class))) {
            SecurityContextHolder.clearContext();
        } else if (!setAuthentication(claims.get())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is restricted by moderation");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean setAuthentication(Claims claims) {
        String login = claims.get("login", String.class);
        List<String> roles = claims.get("accessLevels", List.class);

        String id =  claims.getSubject();
        UUID accountId = UUID.fromString(id);
        if (accountModerationGuard.isRestricted(accountId)) {
            SecurityContextHolder.clearContext();
            return false;
        }

        List<GrantedAuthority> authorities = roleMapper.mapAccessLevels(roles);
        UserPrincipal principal = new UserPrincipal(accountId, login);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        return true;
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }
}
