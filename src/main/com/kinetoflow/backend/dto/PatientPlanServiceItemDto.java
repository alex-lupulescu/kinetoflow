package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.PatientPlanServiceItem;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record PatientPlanServiceItemDto(
        Long id,
        Long serviceId,
        String serviceName,
        Integer totalQuantity,
        Integer remainingQuantity,
        BigDecimal pricePerUnit,
        Boolean isItemActive,
        Boolean isArchived // Added
) {
    public static PatientPlanServiceItemDto fromEntity(PatientPlanServiceItem item) {
        if (item == null || item.getService() == null) return null;
        return new PatientPlanServiceItemDto(
                item.getId(),
                item.getService().getId(),
                item.getService().getName(), // Assumes Service is loaded or accessible
                item.getTotalQuantity(),
                item.getRemainingQuantity(),
                item.getPricePerUnit(),
                item.getIsItemActive(),
                item.getIsArchived() // Map added field
        );
    }
}