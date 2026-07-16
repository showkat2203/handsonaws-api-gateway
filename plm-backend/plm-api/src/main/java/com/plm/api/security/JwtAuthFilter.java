package com.plm.api.security;

import com.plm.service.security.JwtUtil;
import com.plm.service.security.PlmPrincipal;
import com.plm.service.security.PlmPrincipalContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates the {@code Authorization: Bearer <jwt>} header (if present), and populates both
 * Spring Security's context (for {@code @PreAuthorize}-style checks at the transport layer, if
 * ever added) and {@link PlmPrincipalContext} (which the framework-agnostic service layer reads
 * for its own role checks — this is what lets the chatbot process enforce identical authz).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                PlmPrincipal principal = jwtUtil.parse(token);
                PlmPrincipalContext.set(principal);
                List<GrantedAuthority> authorities = principal.getRoles().stream()
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.name()))
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal.getUsername(), null, authorities));
            } catch (JwtException | IllegalArgumentException e) {
                // Leave unauthenticated; SecurityConfig decides whether the endpoint requires auth.
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            PlmPrincipalContext.clear();
        }
    }
}
