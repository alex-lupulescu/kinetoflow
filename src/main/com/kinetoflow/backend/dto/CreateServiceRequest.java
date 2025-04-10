package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record CreateServiceRequest(
        @NotBlank(message = "Service name cannot be blank")
        @Size(max = 150)
        String name,

        String description,

        @NotNull(message = "Duration cannot be null")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        BigDecimal price, // Optional

        @Size(max = 50)
        String category, // Optional

        @NotNull(message = "Active status must be specified")
        Boolean isActive // Require explicit status on creation
) {}