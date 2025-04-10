// src/main/java/com/kinetoflow/backend/service/CalendarService.java (Complete File)

package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.CalendarEventDto;
import com.kinetoflow.backend.dto.CreateTimeBlockRequest; // Import new DTO
import com.kinetoflow.backend.entity.*; // Import TimeBlock, Company etc.
import com.kinetoflow.backend.exception.BadRequestException; // Import exceptions
import com.kinetoflow.backend.exception.ForbiddenException;
import com.kinetoflow.backend.exception.ResourceNotFoundException;
import com.kinetoflow.backend.repository.AppointmentRepository;
import com.kinetoflow.backend.repository.TimeBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final AppointmentRepository appointmentRepository;
    private final TimeBlockRepository timeBlockRepository;

    /**
     * Fetches calendar events (appointments and time blocks) for a specific medic
     * within a given date range.
     */
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getMedicCalendarEvents(User medic, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching calendar events for medic {} between {} and {}", medic.getEmail(), startDate, endDate);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BadRequestException("Invalid date range provided.");
        }

        List<Appointment> appointments = appointmentRepository.findByMedicAndDateTimeRange(medic, startDate, endDate);
        List<TimeBlock> timeBlocks = timeBlockRepository.findByMedicAndDateTimeRange(medic, startDate, endDate);

        Stream<CalendarEventDto> appointmentEvents = appointments.stream()
                .map(appt -> {
                    try {
                        // Ensure necessary fields are loaded (adjust fetch strategy if needed)
                        String patientName = appt.getPatient() != null ? appt.getPatient().getName() : "Unknown Patient";
                        String serviceName = appt.getService() != null ? appt.getService().getName() : "Unknown Service";
                        return CalendarEventDto.fromAppointment(appt); // Assumes fromAppointment handles nulls
                    } catch (Exception e) { log.error("Error mapping appointment {}: {}", appt.getId(), e.getMessage()); return null; }
                })
                .filter(dto -> dto != null);

        Stream<CalendarEventDto> timeBlockEvents = timeBlocks.stream()
                .map(CalendarEventDto::fromTimeBlock);

        return Stream.concat(appointmentEvents, timeBlockEvents).collect(Collectors.toList());
    }

    /**
     * Creates a new Time Block for a medic, marking that time as unavailable.
     *
     * @param medic   The medic creating the block.
     * @param request DTO with start time, end time, and optional reason.
     * @return CalendarEventDto representing the created block.
     */
    @Transactional
    public CalendarEventDto createTimeBlock(User medic, CreateTimeBlockRequest request) {
        log.info("Medic {} creating time block from {} to {}", medic.getEmail(), request.startTime(), request.endTime());

        // --- Validation ---
        if (request.startTime() == null || request.endTime() == null || request.endTime().isBefore(request.startTime())) {
            throw new BadRequestException("Invalid start/end time for time block.");
        }
        // Optional: Add check for overlapping appointments or existing blocks here if needed
        // List<Appointment> overlappingAppts = appointmentRepository.findByMedicAndDateTimeRange(medic, request.startTime(), request.endTime());
        // List<TimeBlock> overlappingBlocks = timeBlockRepository.findByMedicAndDateTimeRange(medic, request.startTime(), request.endTime());
        // if (!overlappingAppts.isEmpty() || !overlappingBlocks.isEmpty()) {
        //     throw new BadRequestException("The selected time range overlaps with an existing event.");
        // }
        Company company = medic.getCompany();
        if (company == null) {
            throw new BadRequestException("Medic must be associated with a company.");
        }
        // --- End Validation ---

        TimeBlock block = TimeBlock.builder()
                .medic(medic)
                .company(company) // Set company
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .build();

        TimeBlock savedBlock = timeBlockRepository.save(block);
        log.info("Time block {} created successfully for medic {}", savedBlock.getId(), medic.getEmail());

        return CalendarEventDto.fromTimeBlock(savedBlock);
    }

    /**
     * Deletes a Time Block, making the time available again.
     * Ensures the block belongs to the requesting medic.
     *
     * @param medic       The medic deleting the block.
     * @param timeBlockId The ID of the TimeBlock to delete.
     */
    @Transactional
    public void deleteTimeBlock(User medic, Long timeBlockId) {
        log.info("Medic {} attempting to delete time block ID {}", medic.getEmail(), timeBlockId);

        TimeBlock block = timeBlockRepository.findById(timeBlockId)
                .orElseThrow(() -> new ResourceNotFoundException("Time block not found with ID: " + timeBlockId));

        // Authorization check: Ensure the block belongs to the medic trying to delete it
        if (!block.getMedic().getId().equals(medic.getId())) {
            log.warn("Forbidden attempt: Medic {} tried to delete time block {} belonging to medic {}", medic.getEmail(), timeBlockId, block.getMedic().getId());
            throw new ForbiddenException("You can only delete your own time blocks.");
        }

        timeBlockRepository.delete(block);
        log.info("Time block {} deleted successfully for medic {}", timeBlockId, medic.getEmail());
    }

    // Add methods later for editing time blocks (reason, times) if needed
}