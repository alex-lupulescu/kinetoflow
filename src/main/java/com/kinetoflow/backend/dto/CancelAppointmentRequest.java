package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data; // Using @Data for a simple DTO with getters/setters

/**
 * DTO for requesting an appointment cancellation.
 */
@Data
public class CancelAppointmentRequest {

    @NotNull(message = "New status must be provided (e.g., CANCELLED_BY_MEDIC or CANCELLED_BY_PATIENT)")
    private AppointmentStatus newStatus;

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    private String cancellationReason; // Optional

    /**
     * Custom validator to ensure the provided status is a valid cancellation status.
     * @return true if the status is a valid cancellation status, false otherwise.
     */
    public boolean isValidCancellationStatus() {
        return newStatus == AppointmentStatus.CANCELLED_BY_MEDIC ||
                newStatus == AppointmentStatus.CANCELLED_BY_PATIENT;
    }
}