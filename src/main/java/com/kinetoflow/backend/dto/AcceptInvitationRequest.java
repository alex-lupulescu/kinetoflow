package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

// DTO for accepting an invitation
@Builder
public record AcceptInvitationRequest(
        @NotBlank(message = "Invitation token cannot be blank")
        String token,

        @NotBlank(message = "Name cannot be blank")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        // Add more password complexity validation if desired (e.g., using regex pattern)
        String password
) {}