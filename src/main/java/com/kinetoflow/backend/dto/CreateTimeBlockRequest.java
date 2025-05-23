package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record CreateTimeBlockRequest(
        @NotNull(message = "Start time cannot be null")
        @FutureOrPresent(message = "Start time must be now or in the future")
        LocalDateTime startTime,

        @NotNull(message = "End time cannot be null")
        @FutureOrPresent(message = "End time must be now or in the future")
        LocalDateTime endTime,

        @Size(max = 255, message = "Reason cannot exceed 255 characters")
        String reason // Optional reason (e.g., "Lunch", "Admin")
        // Add recurrence fields here later if needed
) {
    // Add custom validation: endTime must be after startTime (best done in service)
}