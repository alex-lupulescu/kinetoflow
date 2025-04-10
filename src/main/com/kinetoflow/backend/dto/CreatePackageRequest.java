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
public record CreatePackageRequest(
        @NotBlank(message = "Package name cannot be blank")
        @Size(max = 150)
        String name,

        String description,

        @DecimalMin(value = "0.0", inclusive = true, message = "Total price must be non-negative")
        BigDecimal totalPrice, // Optional

        @NotNull(message = "Active status must be specified") // Explicit status
        Boolean isActive,

        @NotEmpty(message = "Package must include at least one service item")
        @Valid // Validate the items in the list
        List<ServiceItemRequest> items // List of services and their quantities
) {}