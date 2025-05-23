package com.kinetoflow.backend.service;

import com.kinetoflow.backend.entity.Appointment;
import com.kinetoflow.backend.entity.PatientPlanServiceItem;
import com.kinetoflow.backend.enums.AppointmentStatus;
import com.kinetoflow.backend.repository.AppointmentRepository;
import com.kinetoflow.backend.repository.PatientPlanServiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientPlanServiceItemRepository patientPlanServiceItemRepository;

    // Define a cron expression for when the task should run (e.g., every hour at the top of the hour)
    // For testing, you might want something more frequent, like every minute: "0 * * * * ?"
    // For production, perhaps once a day or every few hours: "0 0 */1 * * ?" (every hour)
    @Scheduled(cron = "0 * * * * ?") // Runs every hour
    @Transactional
    public void processPastAppointmentsAndDecrementSessions() {
        log.info("Scheduled task: Processing past appointments to decrement plan sessions...");
        LocalDateTime now = LocalDateTime.now();

        // Define statuses for which sessions should be consumed
        List<AppointmentStatus> applicableStatuses = Arrays.asList(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED);

        List<Appointment> appointmentsToProcess = appointmentRepository.findAppointmentsForSessionDecrement(now, applicableStatuses);

        if (appointmentsToProcess.isEmpty()) {
            log.info("No appointments found requiring session decrement at this time.");
            return;
        }

        log.info("Found {} appointments to process for session decrement.", appointmentsToProcess.size());

        for (Appointment appointment : appointmentsToProcess) {
            PatientPlanServiceItem planItem = appointment.getPatientPlanServiceItem();

            if (planItem != null && planItem.getRemainingQuantity() > 0) {
                log.info("Processing appointment ID: {}. Plan item ID: {}. Current remaining quantity: {}",
                        appointment.getId(), planItem.getId(), planItem.getRemainingQuantity());

                planItem.setRemainingQuantity(planItem.getRemainingQuantity() - 1);
                appointment.setSessionConsumed(true);

                // Optional: If the appointment was still SCHEDULED, mark it as COMPLETED
                // This depends on business logic: Does passing of time automatically complete it?
                if (appointment.getStatus() == AppointmentStatus.SCHEDULED) {
                    log.info("Marking appointment ID: {} as COMPLETED as its time has passed.", appointment.getId());
                    appointment.setStatus(AppointmentStatus.COMPLETED);
                }

                patientPlanServiceItemRepository.save(planItem);
                appointmentRepository.save(appointment);
                log.info("Successfully decremented session for plan item ID: {}. New remaining quantity: {}. Marked appointment ID: {} as sessionConsumed.",
                        planItem.getId(), planItem.getRemainingQuantity(), appointment.getId());
            } else if (planItem != null) {
                log.warn("Appointment ID: {} with plan item ID: {} has no remaining quantity or plan item is null. Session not decremented.",
                        appointment.getId(), planItem.getId());
                 // Mark as consumed even if no quantity to prevent reprocessing, or handle as error
                appointment.setSessionConsumed(true);
                appointmentRepository.save(appointment);
            }
        }
        log.info("Finished processing past appointments for session decrement.");
    }
} 