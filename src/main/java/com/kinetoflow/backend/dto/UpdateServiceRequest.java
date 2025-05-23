package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record UpdateServiceRequest(
        @NotBlank(message = "Service name cannot be blank")
        @Size(max = 150)
        String name, // Include all fields that *can* be updated

        String description,

        @NotNull(message = "Duration cannot be null")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        BigDecimal price, // Allow setting price to null if desired

        @Size(max = 50)
        String category,

        @NotNull(message = "Active status must be specified")
        Boolean isActive // Always require status in update? Or use separate endpoint? Let's require it for now.
) {}