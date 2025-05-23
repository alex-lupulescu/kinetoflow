package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.enums.UserRole;
import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        Long userId,
        String name,
        String email,
        UserRole role,
        Long companyId // Can be null for APP_ADMIN
) {}