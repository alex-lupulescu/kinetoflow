package com.kinetoflow.backend.config;

import com.kinetoflow.backend.security.jwt.JwtAuthenticationEntryPoint;
import com.kinetoflow.backend.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration class.
 * Enables web security, method security, and configures filters, providers, and access rules.
 */
@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true) // Enables @PreAuthorize, @Secured, @RolesAllowed
@RequiredArgsConstructor // Lombok constructor injection for final fields
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService; // Our UserDetailsServiceImpl
    private final JwtAuthenticationEntryPoint unauthorizedHandler; // Handles 401 errors

    @Value("${app.frontend.url}")
    private String frontendUrl; // URL of the Vue.js frontend

    // Define public endpoints that do not require authentication
    private static final String[] PUBLIC_MATCHERS = {
            "/api/auth/**",                       // Login endpoints
            "/api/invitations/details/**",        // Get invitation details (public GET)
            "/api/invitations/accept",            // Accept invitation (public POST)
            // Swagger/OpenAPI documentation endpoints
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            // Add other public endpoints like health checks if needed
            // "/actuator/health"
    };

    /**
     * Configures the main security filter chain.
     * Defines CORS, CSRF, session management, exception handling, request authorization rules,
     * and registers the JWT authentication filter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure CORS using the corsConfigurationSource bean
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF protection as we are using stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                // Configure custom entry point for handling authentication exceptions (401 Unauthorized)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                // Configure session management to be stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure authorization rules for HTTP requests
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_MATCHERS).permitAll() // Allow public access to specified endpoints
                        .requestMatchers(HttpMethod.OPTIONS).permitAll() // Allow CORS pre-flight requests
                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                // Set the custom authentication provider (DaoAuthenticationProvider)
                .authenticationProvider(authenticationProvider())
                // Add the custom JWT filter before the standard UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS).
     * Allows requests from the specified frontend URL with common methods and headers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow requests only from the specified frontend origin
        configuration.setAllowedOrigins(List.of(frontendUrl));
        // Allow standard HTTP methods + PATCH
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Allow common headers necessary for JWT auth and content type negotiation
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        // Allow credentials (cookies, authorization headers) - important for JWT
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this CORS configuration to all paths under /api/
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * Creates the AuthenticationProvider bean (DaoAuthenticationProvider).
     * Sets the UserDetailsService (to load user data) and PasswordEncoder (to verify passwords).
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // Our custom UserDetailsServiceImpl
        authProvider.setPasswordEncoder(passwordEncoder()); // Use BCrypt for password hashing
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager bean from the AuthenticationConfiguration.
     * Needed for manually authenticating users in the AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the PasswordEncoder bean (BCrypt).
     * Used for hashing passwords during user registration/update and verifying passwords during login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}