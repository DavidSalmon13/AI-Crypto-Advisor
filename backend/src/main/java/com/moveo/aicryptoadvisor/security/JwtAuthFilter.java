package com.moveo.aicryptoadvisor.security;

import com.moveo.aicryptoadvisor.entity.User;
import com.moveo.aicryptoadvisor.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                UUID userId = UUID.fromString(jwtService.parseClaims(token).getSubject());
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    userRepository.findById(userId).ifPresent(user -> authenticate(user, request));
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // invalid/expired/malformed token -> leave request unauthenticated;
                // AuthorizationFilter + JsonAuthenticationEntryPoint handle the 401 for protected routes.
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(User user, HttpServletRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
