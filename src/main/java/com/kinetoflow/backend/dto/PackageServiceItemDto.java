package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.PackageServiceItem;
import lombok.Builder;

@Builder
public record PackageServiceItemDto(
        Long itemId, // ID of the link table item itself
        Long serviceId,
        String serviceName, // Include name for display
        Integer quantity
) {
    public static PackageServiceItemDto fromEntity(PackageServiceItem item) {
        if (item == null || item.getService() == null) return null;
        return new PackageServiceItemDto(
                item.getId(),
                item.getService().getId(),
                item.getService().getName(), // Eagerly load or ensure service proxy is initialized
                item.getQuantity()
        );
    }
}