package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.AppointmentDto;
import com.kinetoflow.backend.dto.CancelAppointmentRequest;
import com.kinetoflow.backend.dto.CreateAppointmentRequest;
import com.kinetoflow.backend.entity.*;
import com.kinetoflow.backend.enums.AppointmentStatus;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.exception.ForbiddenException;
import com.kinetoflow.backend.exception.ResourceNotFoundException;
import com.kinetoflow.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final PatientPlanServiceItemRepository patientPlanServiceItemRepository;
    private final TimeBlockRepository timeBlockRepository;

    @Transactional
    public AppointmentDto createAppointment(User medic, CreateAppointmentRequest request) {
        log.info("Medic {} creating appointment for patient {}", medic.getEmail(), request.patientId());
        Company company = medic.getCompany();
        if (company == null) {
            throw new BadRequestException("Medic must belong to a company.");
        }
        if (request.scheduledStartTime() == null || request.scheduledEndTime() == null || !request.scheduledEndTime().isAfter(request.scheduledStartTime())) {
            throw new BadRequestException("Invalid start/end time for appointment. End time must be after start time.");
        }

        User patient = userRepository.findById(request.patientId()).orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));
        if (patient.getRole() != UserRole.USER) {
            throw new BadRequestException("Selected user is not a patient.");
        }
        if (!patient.isActive()) {
            throw new BadRequestException("Cannot schedule for inactive patient.");
        }
        if (patient.getCompany() == null || !patient.getCompany().getId().equals(company.getId())) {
            throw new BadRequestException("Patient not in medic's company.");
        }
        if (patient.getAssignedMedic() == null || !patient.getAssignedMedic().getId().equals(medic.getId())) {
            throw new ForbiddenException("Can only create appointments for assigned patients.");
        }

        Service service = serviceRepository.findById(request.serviceId()).orElseThrow(() -> new ResourceNotFoundException("Service not found: " + request.serviceId()));
        if (!service.getIsActive()) {
            throw new BadRequestException("Cannot schedule for inactive service: " + service.getName());
        }
        if (service.getCompany() == null || !service.getCompany().getId().equals(company.getId())) {
            throw new BadRequestException("Service not in medic's company.");
        }

        PatientPlanServiceItem planItem = null;
        if (request.patientPlanServiceItemId() != null) {
            planItem = patientPlanServiceItemRepository.findDetailsById(request.patientPlanServiceItemId()).orElseThrow(() -> new ResourceNotFoundException("Plan Item not found: " + request.patientPlanServiceItemId()));
            if (planItem.getPatientPlan() == null || planItem.getPatientPlan().getPatient() == null || !planItem.getPatientPlan().getPatient().getId().equals(patient.getId())) {
                throw new BadRequestException("Plan item not for this patient.");
            }
            if (!planItem.getPatientPlan().getIsActive() || Boolean.TRUE.equals(planItem.getPatientPlan().getIsArchived())) {
                throw new BadRequestException("Cannot use item from inactive/archived plan.");
            }
            if (!planItem.getIsItemActive() || Boolean.TRUE.equals(planItem.getIsArchived())) {
                throw new BadRequestException("Selected plan item inactive/archived.");
            }
            if (planItem.getService() == null || !planItem.getService().getId().equals(service.getId())) {
                throw new BadRequestException("Plan item service mismatch.");
            }
            if (planItem.getRemainingQuantity() <= 0) {
                throw new BadRequestException("No remaining sessions for: " + service.getName());
            }
            log.info("Appointment consumes session from Plan Item ID: {}", planItem.getId());
        }

        checkTimeSlotAvailability(medic, patient, request.scheduledStartTime(), request.scheduledEndTime(), null);

        Appointment appointment = Appointment.builder()
                .patient(patient).medic(medic).service(service).company(company)
                .scheduledStartTime(request.scheduledStartTime()).scheduledEndTime(request.scheduledEndTime())
                .status(AppointmentStatus.SCHEDULED).notes(request.notes())
                .patientPlanServiceItem(planItem).build();
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Appointment {} created successfully for patient {} with medic {}", savedAppointment.getId(), patient.getId(), medic.getId());
        return AppointmentDto.fromEntity(savedAppointment);
    }

    /**
     * Cancels an appointment.
     * Performed by the Medic to whom the appointment is assigned.
     *
     * @param medic         The authenticated medic performing the cancellation.
     * @param appointmentId The ID of the appointment to cancel.
     * @param request       DTO containing the new status (must be a cancelled status) and optional reason.
     * @return DTO of the updated (cancelled) appointment.
     */
    @Transactional
    public AppointmentDto cancelAppointmentByMedic(User medic, Long appointmentId, CancelAppointmentRequest request) {
        log.info("Medic {} attempting to cancel appointment ID {}", medic.getEmail(), appointmentId);

        if (request == null || !request.isValidCancellationStatus()) {
            throw new BadRequestException("Invalid cancellation status provided. Must be CANCELLED_BY_MEDIC or CANCELLED_BY_PATIENT.");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Authorization: Ensure the appointment belongs to this medic
        if (!appointment.getMedic().getId().equals(medic.getId())) {
            log.warn("Forbidden: Medic {} tried to cancel appointment {} not assigned to them.", medic.getEmail(), appointmentId);
            throw new ForbiddenException("You can only cancel your own appointments.");
        }

        // Business Rule: Can only cancel appointments that are currently SCHEDULED
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Cannot cancel appointment: Current status is " + appointment.getStatus() + ". Only SCHEDULED appointments can be cancelled.");
        }

        appointment.setStatus(request.getNewStatus());
        if (request.getCancellationReason() != null && !request.getCancellationReason().isBlank()) {
            String existingNotes = appointment.getNotes() != null ? appointment.getNotes() + "\n" : "";
            appointment.setNotes(existingNotes + "Cancellation Reason (" + request.getNewStatus() + "): " + request.getCancellationReason());
        }

        Appointment cancelledAppointment = appointmentRepository.save(appointment);
        log.info("Appointment {} cancelled successfully by medic {}. New status: {}", appointmentId, medic.getEmail(), request.getNewStatus());
        return AppointmentDto.fromEntity(cancelledAppointment);
    }

    private void checkTimeSlotAvailability(User medic, User patient, LocalDateTime start, LocalDateTime end, Long excludeAppointmentId) {
        // List<Appointment> overlappingApptsMedic = appointmentRepository.findByMedicAndDateTimeRange(medic, start, end).stream().filter(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId)).toList();
        // if (!overlappingApptsMedic.isEmpty()) {
        //     throw new BadRequestException("Medic is unavailable (overlaps appointment).");
        // }
        List<TimeBlock> overlappingBlocksMedic = timeBlockRepository.findByMedicAndDateTimeRange(medic, start, end);
        if (!overlappingBlocksMedic.isEmpty()) {
            throw new BadRequestException("Medic has blocked off time.");
        }
        List<Appointment> overlappingApptsPatient = appointmentRepository.findByPatientAndDateTimeRange(patient, start, end).stream().filter(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId)).toList();
        if (!overlappingApptsPatient.isEmpty()) {
            throw new BadRequestException("Patient has another appointment.");
        }
    }

    @Transactional
    public void deleteAppointment(User medic, Long appointmentId) {
        log.info("Medic {} attempting to delete appointment {}", medic.getEmail(), appointmentId);
        
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Authorization: Ensure the appointment belongs to this medic
        if (!appointment.getMedic().getId().equals(medic.getId())) {
            log.warn("Forbidden: Medic {} tried to delete appointment {} not assigned to them.", medic.getEmail(), appointmentId);
            throw new ForbiddenException("You can only delete your own appointments.");
        }

        // Business Rule: Can only delete appointments that are in the future
        if (appointment.getScheduledStartTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot delete past appointments.");
        }

        // Hard delete the appointment
        appointmentRepository.delete(appointment);
        log.info("Appointment {} deleted successfully by medic {}", appointmentId, medic.getEmail());
    }

    // TODO: Implement completeAppointment, updateAppointment
}