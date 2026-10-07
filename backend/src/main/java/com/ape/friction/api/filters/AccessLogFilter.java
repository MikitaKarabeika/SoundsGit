package com.ape.friction.api.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long start = System.currentTimeMillis();


            filterChain.doFilter(request, response);


            long duration = System.currentTimeMillis() - start;

            String ip = getClientIp(request);

            log.info("{} [{}] \"{} {}\" {} {}ms",
                    ip,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getProtocol(),
                    response.getStatus(),
                    duration
            );

    }

    private String getClientIp(HttpServletRequest request) {

        String forwarded =
                request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}
