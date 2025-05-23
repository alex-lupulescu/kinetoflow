package com.kinetoflow.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude; // Include non-null fields
import com.kinetoflow.backend.entity.Appointment;
import com.kinetoflow.backend.entity.TimeBlock;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter // Only getters needed
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON output
public class CalendarEventDto {
    private String id; // Combined type and ID, e.g., "appt-123", "block-45"
    private String title; // e.g., "Physio - John Doe", "Lunch Break"
    private LocalDateTime start;
    private LocalDateTime end;
    private String type; // "appointment" or "block"
    private String status; // Appointment status (SCHEDULED, COMPLETED etc.) or null for blocks
    private String color; // Optional: Color coding for event types/status
    private Long patientId; // If it's an appointment
    private String patientName; // If it's an appointment
    private Long serviceId; // If it's an appointment
    private String serviceName; // If it's an appointment
    private String notes; // Appointment notes or block reason

    // Static factory for Appointments
    public static CalendarEventDto fromAppointment(Appointment appt) {
        if (appt == null) return null;
        return CalendarEventDto.builder()
                .id("appt-" + appt.getId())
                .title(appt.getService().getName() + " - " + appt.getPatient().getName())
                .start(appt.getScheduledStartTime())
                .end(appt.getScheduledEndTime())
                .type("appointment")
                .status(appt.getStatus() != null ? appt.getStatus().name() : null)
                .color(getAppointmentColor(appt)) // Assign color based on status
                .patientId(appt.getPatient().getId())
                .patientName(appt.getPatient().getName())
                .serviceId(appt.getService().getId())
                .serviceName(appt.getService().getName())
                .notes(appt.getNotes())
                .build();
    }

    // Static factory for TimeBlocks
    public static CalendarEventDto fromTimeBlock(TimeBlock block) {
        if (block == null) return null;
        return CalendarEventDto.builder()
                .id("block-" + block.getId())
                .title(block.getReason() != null && !block.getReason().isBlank() ? block.getReason() : "Blocked Time")
                .start(block.getStartTime())
                .end(block.getEndTime())
                .type("block")
                .color("#6c757d") // Example: Grey color for blocks
                // status, patientId/Name, serviceId/Name are null
                .notes(block.getReason())
                .build();
    }

    // Helper for color coding appointments (customize as needed)
    private static String getAppointmentColor(Appointment appt) {
        if (appt.getStatus() == null) return "#3498DB"; // Default blue (e.g., SCHEDULED)
        switch (appt.getStatus()) {
            case COMPLETED: return "#2ECC71"; // Green
            case CANCELLED_BY_MEDIC:
            case CANCELLED_BY_PATIENT: return "#E74C3C"; // Red
            case NO_SHOW: return "#F39C12"; // Orange
            case SCHEDULED:
            default: return "#3498DB"; // Blue
        }
    }
}