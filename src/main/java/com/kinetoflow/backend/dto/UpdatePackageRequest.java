package com.kinetoflow.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull; // Import NotNull
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Builder
public record UpdatePackageRequest(
        @NotBlank(message = "Package name cannot be blank")
        @Size(max = 150)
        String name, // Include all updatable fields

        String description,

        @DecimalMin(value = "0.0", inclusive = true, message = "Total price must be non-negative")
        BigDecimal totalPrice, // Allow setting to null if desired

        @NotNull(message = "Active status must be specified") // Explicit status
        Boolean isActive,

        @NotEmpty(message = "Package must include at least one service item")
        @Valid
        List<ServiceItemRequest> items // Provide the full updated list of items
) {}