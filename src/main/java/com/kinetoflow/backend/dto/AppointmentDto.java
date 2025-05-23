package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.Appointment;
import com.kinetoflow.backend.enums.AppointmentStatus; // Import enum
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record AppointmentDto(
        Long id,
        Long patientId,
        String patientName,
        Long medicId,
        String medicName,
        Long serviceId,
        String serviceName,
        Long companyId,
        LocalDateTime scheduledStartTime,
        LocalDateTime scheduledEndTime,
        LocalDateTime actualStartTime,
        LocalDateTime actualEndTime,
        AppointmentStatus status, // Use enum
        String notes,
        Long patientPlanServiceItemId, // ID of the item being consumed (if any)
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppointmentDto fromEntity(Appointment appt) {
        if (appt == null) return null;
        // Assumes patient, medic, service relations are loaded or accessible
        return new AppointmentDto(
                appt.getId(),
                appt.getPatient() != null ? appt.getPatient().getId() : null,
                appt.getPatient() != null ? appt.getPatient().getName() : null,
                appt.getMedic() != null ? appt.getMedic().getId() : null,
                appt.getMedic() != null ? appt.getMedic().getName() : null,
                appt.getService() != null ? appt.getService().getId() : null,
                appt.getService() != null ? appt.getService().getName() : null,
                appt.getCompany() != null ? appt.getCompany().getId() : null,
                appt.getScheduledStartTime(),
                appt.getScheduledEndTime(),
                appt.getActualStartTime(),
                appt.getActualEndTime(),
                appt.getStatus(),
                appt.getNotes(),
                appt.getPatientPlanServiceItem() != null ? appt.getPatientPlanServiceItem().getId() : null,
                appt.getCreatedAt(),
                appt.getUpdatedAt()
        );
    }
}