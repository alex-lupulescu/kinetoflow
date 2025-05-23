package com.kinetoflow.backend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor // Lombok constructor injection
@Slf4j // Lombok logger
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Spring Security interface

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Check if Authorization header is present and correctly formatted
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Continue chain if no token
            return;
        }

        jwt = authHeader.substring(7); // Extract token after "Bearer "

        try {
            userEmail = jwtService.extractUsername(jwt);

            // 2. Check if email is extracted and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details from database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 3. Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials are null for JWT
                            userDetails.getAuthorities()
                    );
                    // Set details (like IP address, session ID - less relevant for stateless)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    // Update SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("User '{}' authenticated successfully via JWT.", userEmail);
                } else {
                    log.warn("Invalid JWT token received for user '{}'.", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Let the exception handling mechanism (e.g., JwtAuthenticationEntryPoint) deal with it further down the chain if needed,
            // but clear the context just in case.
            SecurityContextHolder.clearContext();
            // Depending on strictness, you might want to immediately send an error response here or let it proceed
            // to the AuthenticationEntryPoint. For simplicity, we let it proceed.
        }

        // 4. Continue filter chain
        filterChain.doFilter(request, response);
    }
}