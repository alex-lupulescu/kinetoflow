package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.PatientPlan;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@Builder
public record PatientPlanDto(
        Long id,
        Long patientId,
        String patientName,
        Long assignedById,
        String assignedByName,
        Long companyId,
        Long originatingPackageId,
        String originatingPackageName,
        Boolean isActive,
        Boolean isArchived, // Added
        LocalDateTime assignedDate,
        LocalDateTime expiryDate,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PatientPlanServiceItemDto> serviceItems
) {
    public static PatientPlanDto fromEntity(PatientPlan plan) {
        if (plan == null) return null;

        List<PatientPlanServiceItemDto> itemDtos = (plan.getServiceItems() == null) ? Collections.emptyList() :
                plan.getServiceItems().stream()
                        // Optionally filter out archived items here if DTO should only show non-archived
                        // .filter(item -> Boolean.FALSE.equals(item.getIsArchived()))
                        .map(PatientPlanServiceItemDto::fromEntity)
                        .collect(Collectors.toList());

        return new PatientPlanDto(
                plan.getId(),
                plan.getPatient() != null ? plan.getPatient().getId() : null,
                plan.getPatient() != null ? plan.getPatient().getName() : null,
                plan.getAssignedBy() != null ? plan.getAssignedBy().getId() : null,
                plan.getAssignedBy() != null ? plan.getAssignedBy().getName() : null,
                plan.getCompany() != null ? plan.getCompany().getId() : null,
                plan.getOriginatingPackage() != null ? plan.getOriginatingPackage().getId() : null,
                plan.getOriginatingPackage() != null ? plan.getOriginatingPackage().getName() : null,
                plan.getIsActive(),
                plan.getIsArchived(), // Map added field
                plan.getAssignedDate(),
                plan.getExpiryDate(),
                plan.getNotes(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                itemDtos
        );
    }
}