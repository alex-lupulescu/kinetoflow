package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder; // Use Builder for consistency

// DTO for updating quantities by Medic/Admin (correction tool)
@Builder
public record UpdatePlanItemQuantitiesRequest(
        @NotNull(message = "Total quantity must be specified")
        @Min(value = 1, message = "Total quantity must be at least 1")
        Integer totalQuantity,

        @NotNull(message = "Remaining quantity must be specified")
        @Min(value = 0, message = "Remaining quantity cannot be negative")
        Integer remainingQuantity
        // Consider adding a 'reason' field for audit trail
        // String reasonForChange;
) {}