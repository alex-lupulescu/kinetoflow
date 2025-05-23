package com.kinetoflow.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDateTime;
import java.util.List; // Use List for ordered items if needed, else Set

public record AssignPlanRequestDto(
        // Option 1: Assign based on a Package Template
        Long packageId, // ID of the package to assign (mutually exclusive with items below)

        // Option 2: Assign individual Service Items
        // Use @Valid to trigger validation on nested objects
        @Valid
        List<ServiceItemRequest> serviceItems, // List of { serviceId, quantity } (mutually exclusive with packageId)

        // Common fields
        String notes, // Optional notes for the plan

        // Dates need careful handling - allow null? Require start?
        LocalDateTime assignedDate, // Can default to now() server-side

        @FutureOrPresent(message = "Expiry date must be in the present or future")
        LocalDateTime expiryDate // Optional expiry
) {
    // Add custom validation: Either packageId OR serviceItems must be provided, but not both.
    // This is typically done in the Service layer, not easily with annotations here.
}