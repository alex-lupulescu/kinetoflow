package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a service and its quantity when creating/updating a package.
 * Used within CreatePackageRequest and UpdatePackageRequest.
 */
public record ServiceItemRequest(
        @NotNull(message = "Service ID cannot be null")
        Long serviceId,

        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {}