package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.Package;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections; // Import Collections for empty list

@Builder
public record PackageDto(
        Long id,
        String name,
        String description,
        BigDecimal totalPrice,
        Boolean isActive,
        Long companyId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PackageServiceItemDto> items // Include list of items DTOs
) {
    public static PackageDto fromEntity(Package pkg) {
        if (pkg == null) return null;

        // Ensure items collection is initialized before streaming
        List<PackageServiceItemDto> itemDtos = (pkg.getItems() == null) ? Collections.emptyList() :
                pkg.getItems().stream()
                        .map(PackageServiceItemDto::fromEntity)
                        .collect(Collectors.toList());

        return new PackageDto(
                pkg.getId(),
                pkg.getName(),
                pkg.getDescription(),
                pkg.getTotalPrice(),
                pkg.getIsActive(),
                pkg.getCompany() != null ? pkg.getCompany().getId() : null,
                pkg.getCreatedAt(),
                pkg.getUpdatedAt(),
                itemDtos
        );
    }
}