package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.enums.UserRole;
import lombok.Builder;

// DTO for returning invitation details based on token
@Builder
public record InvitationDetailsResponse(
        String email, // The invited email (read-only for user)
        UserRole role, // The assigned role
        String companyName, // Name of the company they are joining (if applicable)
        String inviterName // Name of the person who invited them (optional)
) {}