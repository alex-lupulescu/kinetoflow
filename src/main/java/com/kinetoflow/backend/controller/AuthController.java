package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.AuthResponse;
import com.kinetoflow.backend.dto.LoginRequest;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.security.jwt.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    // No need to inject UserRepository/UserService directly if just logging in,
    // AuthenticationManager handles loading via UserDetailsService

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        log.info("Authentication attempt for user: {}", loginRequest.email());

        // Perform authentication using Spring Security's AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        // If authentication is successful, set it in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Retrieve the authenticated user details (our User entity implements UserDetails)
        User userDetails = (User) authentication.getPrincipal();

        // Generate JWT token
        String jwt = jwtService.generateToken(userDetails);

        log.info("User '{}' authenticated successfully. Role: {}", userDetails.getEmail(), userDetails.getRole());

        // Build response DTO
        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .userId(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .companyId(userDetails.getCompany() != null ? userDetails.getCompany().getId() : null)
                .build();

        return ResponseEntity.ok(authResponse);
    }

    // Optional: Add a /refresh token endpoint later if needed
    // Optional: Add a /me endpoint to get current user details based on token
}