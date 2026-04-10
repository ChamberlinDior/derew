package com.oviro.security;

import com.oviro.repository.SessionTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SessionTokenRepository sessionTokenRepository;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        boolean skip =
                path.startsWith("/auth/")
                        || path.startsWith("/uploads/")
                        || path.startsWith("/api-docs")
                        || path.startsWith("/swagger-ui")
                        || path.equals("/swagger-ui.html");

        if (skip) {
            log.debug("JWT filter skipped for path={}", path);
        }

        return skip;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        try {
            String token = extractToken(request);

            log.debug("JWT filter processing path={}, hasToken={}", path, StringUtils.hasText(token));

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String tokenType = jwtUtil.extractTokenType(token);

                if ("ACCESS".equals(tokenType)) {
                    UUID userId = jwtUtil.extractUserId(token);
                    String role = jwtUtil.extractRole(token);

                    boolean sessionActive = sessionTokenRepository
                            .findByToken(token)
                            .map(s -> s.isActive())
                            .orElse(false);

                    log.debug(
                            "JWT token parsed path={}, userId={}, role={}, sessionActive={}",
                            path,
                            userId,
                            role,
                            sessionActive
                    );

                    if (sessionActive && SecurityContextHolder.getContext().getAuthentication() == null) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        log.debug("JWT authentication set for userId={} on path={}", userId, path);
                    }
                } else {
                    log.debug("JWT ignored because tokenType={} on path={}", tokenType, path);
                }
            }
        } catch (Exception ex) {
            log.debug("Impossible d'authentifier l'utilisateur via JWT sur path={} : {}", path, ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}