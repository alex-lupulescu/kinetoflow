package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record CreateAppointmentRequest(
        @NotNull(message = "Patient ID cannot be null")
        Long patientId,

        @NotNull(message = "Service ID cannot be null")
        Long serviceId,

        @NotNull(message = "Start time cannot be null")
        @FutureOrPresent(message = "Start time must be now or in the future")
        LocalDateTime scheduledStartTime,

        @NotNull(message = "End time cannot be null")
        @FutureOrPresent(message = "End time must be now or in the future")
        LocalDateTime scheduledEndTime,

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes, // Optional

        Long patientPlanServiceItemId // Optional: Link to the specific item being consumed
) {
    // Add custom validation: endTime must be after startTime (in service layer)
}