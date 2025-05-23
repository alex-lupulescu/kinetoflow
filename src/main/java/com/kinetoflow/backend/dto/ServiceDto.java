package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.Service;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ServiceDto(
        Long id,
        String name,
        String description,
        Integer durationMinutes,
        BigDecimal price,
        String category,
        Boolean isActive,
        Long companyId, // Include company ID for context
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ServiceDto fromEntity(Service service) {
        if (service == null) return null;
        return new ServiceDto(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.getCategory(),
                service.getIsActive(),
                service.getCompany() != null ? service.getCompany().getId() : null,
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}