package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

// DTO for sending an invitation
@Builder
public record InvitationRequest(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invitee email must be valid")
        String email,

        @NotNull(message = "Role cannot be null")
        UserRole role, // Role to assign (COMPANY_ADMIN, MEDIC, USER) - APP_ADMIN likely created manually

        // Company ID is required if the inviter is a COMPANY_ADMIN
        // It might be automatically determined based on the logged-in COMPANY_ADMIN
        Long companyId, // Make validation conditional in service layer

        String inviterName // Optional: Can be derived from the logged-in user server-side
) {}