package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.User;
import lombok.Builder;

/**
 * DTO for summarizing user information, typically used in lists.
 */
@Builder
public record UserSummaryDto(
        Long id,
        String name,
        String email,
        boolean isActive,
        String role, // Include role for context if needed
        Long assignedMedicId, // For patients, ID of the assigned medic
        String assignedMedicName // For patients, name of the assigned medic
) {
    public static UserSummaryDto fromEntity(User user) {
        if (user == null) return null;
        return UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .isActive(user.isActive())
                .role(user.getRole().name())
                .assignedMedicId(user.getAssignedMedic() != null ? user.getAssignedMedic().getId() : null)
                .assignedMedicName(user.getAssignedMedic() != null ? user.getAssignedMedic().getName() : null)
                .build();
    }
}