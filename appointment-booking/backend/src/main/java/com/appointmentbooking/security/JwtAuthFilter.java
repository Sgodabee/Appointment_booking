package com.appointmentbooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request. Extracts the Bearer token from the Authorization
 * header, validates it, and populates the SecurityContext so downstream
 * filters and controllers can call authentication.getPrincipal().
 *
 * Unauthenticated requests pass through — the SecurityConfig authorization
 * rules decide whether the route requires authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.parseAndValidate(token);

            String subject = claims.getSubject();
            String role    = (String) claims.get(JwtService.CLAIM_ROLE);

            // Grant ROLE_<role> so Spring Security @PreAuthorize("hasRole(...)") works
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var auth      = new UsernamePasswordAuthenticationToken(subject, null, List.of(authority));

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            // Don't set authentication — let SecurityConfig rules reject if needed
        }

        chain.doFilter(request, response);
    }
}
