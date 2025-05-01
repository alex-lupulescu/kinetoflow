package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.AppointmentDto;
import com.kinetoflow.backend.dto.CreateAppointmentRequest;
import com.kinetoflow.backend.entity.*; // User, Company, Service, Appointment, PatientPlanServiceItem
import com.kinetoflow.backend.enums.AppointmentStatus;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.exception.ForbiddenException;
import com.kinetoflow.backend.exception.ResourceNotFoundException;
import com.kinetoflow.backend.repository.*; // AppointmentRepository, UserRepository, ServiceRepository, PatientPlanServiceItemRepository
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
    private final PatientPlanServiceItemRepository patientPlanServiceItemRepository; // To link consumed item
    private final TimeBlockRepository timeBlockRepository; // To check for conflicts

    @Transactional
    public AppointmentDto createAppointment(User medic, CreateAppointmentRequest request) {
        log.info("Medic {} creating appointment for patient {}", medic.getEmail(), request.patientId());

        // --- Validation ---
        Company company = medic.getCompany();
        if (company == null) { throw new BadRequestException("Medic must belong to a company."); }
        if (request.scheduledStartTime() == null || request.scheduledEndTime() == null || request.scheduledEndTime().isBefore(request.scheduledStartTime()) || request.scheduledEndTime().isEqual(request.scheduledStartTime())) {
            throw new BadRequestException("Invalid start/end time for appointment.");
        }

        // Validate Patient
        User patient = userRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));
        if (patient.getRole() != UserRole.USER) { throw new BadRequestException("Selected user is not a patient."); }
        if (!patient.isActive()) { throw new BadRequestException("Cannot schedule appointment for inactive patient."); }
        if (patient.getCompany() == null || !patient.getCompany().getId().equals(company.getId())) { throw new BadRequestException("Patient does not belong to medic's company."); }
        // Authorization: Ensure medic is assigned to this patient
        if (patient.getAssignedMedic() == null || !patient.getAssignedMedic().getId().equals(medic.getId())) {
            log.warn("Auth fail: Medic {} tried creating appointment for unassigned patient {}", medic.getEmail(), patient.getEmail());
            throw new ForbiddenException("You can only create appointments for patients assigned to you.");
        }

        // Validate Service
        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + request.serviceId()));
        if (!service.getIsActive()) { throw new BadRequestException("Cannot schedule appointment for inactive service: " + service.getName()); }
        if (!service.getCompany().getId().equals(company.getId())) { throw new BadRequestException("Service does not belong to medic's company."); }

        // Validate Optional Patient Plan Service Item
        PatientPlanServiceItem planItem = null;
        if (request.patientPlanServiceItemId() != null) {
            planItem = patientPlanServiceItemRepository.findDetailsById(request.patientPlanServiceItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient Plan Service Item not found: " + request.patientPlanServiceItemId()));
            // Ensure item belongs to the correct patient and plan is active/not archived
            if (!planItem.getPatientPlan().getPatient().getId().equals(patient.getId())) { throw new BadRequestException("Selected plan item does not belong to this patient."); }
            if (!planItem.getPatientPlan().getIsActive() || Boolean.TRUE.equals(planItem.getPatientPlan().getIsArchived())) { throw new BadRequestException("Cannot use item from an inactive or archived plan."); }
            if (!planItem.getIsItemActive() || Boolean.TRUE.equals(planItem.getIsArchived())) { throw new BadRequestException("Selected plan item is inactive or archived."); }
            // Ensure the item's service matches the requested serviceId
            if (!planItem.getService().getId().equals(service.getId())){ throw new BadRequestException("Selected plan item service does not match the requested appointment service."); }
            // Ensure there are remaining sessions
            if (planItem.getRemainingQuantity() <= 0) { throw new BadRequestException("No remaining sessions for the selected plan item: " + service.getName()); }
            log.info("Appointment will consume 1 session from Plan Item ID: {}", planItem.getId());
        }

        // --- Conflict Check ---
        // Check for overlapping appointments for the MEDIC
        List<Appointment> overlappingApptsMedic = appointmentRepository.findByMedicAndDateTimeRange(medic, request.scheduledStartTime(), request.scheduledEndTime());
        if (!overlappingApptsMedic.isEmpty()) { throw new BadRequestException("Selected time overlaps with an existing appointment for the medic."); }
        // Check for overlapping blocks for the MEDIC
        List<TimeBlock> overlappingBlocksMedic = timeBlockRepository.findByMedicAndDateTimeRange(medic, request.scheduledStartTime(), request.scheduledEndTime());
        if (!overlappingBlocksMedic.isEmpty()) { throw new BadRequestException("Selected time overlaps with a blocked period for the medic."); }
        // Optional: Check for overlapping appointments for the PATIENT
        List<Appointment> overlappingApptsPatient = appointmentRepository.findByPatientAndDateTimeRange(patient, request.scheduledStartTime(), request.scheduledEndTime());
        if (!overlappingApptsPatient.isEmpty()) { throw new BadRequestException("Selected time overlaps with an existing appointment for the patient."); }
        // --- End Conflict Check ---


        // --- Create Appointment ---
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .medic(medic)
                .service(service)
                .company(company)
                .scheduledStartTime(request.scheduledStartTime())
                .scheduledEndTime(request.scheduledEndTime())
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes())
                .patientPlanServiceItem(planItem) // Link the item if provided
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Appointment {} created successfully.", savedAppointment.getId());

        // Note: Session decrement happens on COMPLETION, not creation.

        return AppointmentDto.fromEntity(savedAppointment);
    }

    // Add completeAppointment, cancelAppointment, updateAppointment methods later
}