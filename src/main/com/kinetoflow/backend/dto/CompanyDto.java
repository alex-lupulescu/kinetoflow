package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.Company; // Import Company entity
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for representing Company information in API responses.
 */
@Builder // Lombok builder for easy object creation
public record CompanyDto(
        Long id,
        String name,
        String address,
        LocalDateTime createdAt
        // Note: We are deliberately excluding the 'users' list for performance
        // and to avoid circular dependencies in basic listings.
        // A separate endpoint could provide company users if needed.
) {
    // Static factory method to map from Company entity to CompanyDto
    public static CompanyDto fromEntity(Company company) {
        if (company == null) {
            return null;
        }
        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .address(company.getAddress())
                .createdAt(company.getCreatedAt())
                .build();
    }
}