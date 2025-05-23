package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.AppointmentDto;
import com.kinetoflow.backend.dto.AssignPlanRequestDto;
import com.kinetoflow.backend.dto.CalendarEventDto;
import com.kinetoflow.backend.dto.CancelAppointmentRequest;
import com.kinetoflow.backend.dto.CreateAppointmentRequest;
import com.kinetoflow.backend.dto.CreateTimeBlockRequest;
import com.kinetoflow.backend.dto.InvitationRequest;
import com.kinetoflow.backend.dto.MedicWorkingHoursDto;
import com.kinetoflow.backend.dto.PackageDto;
import com.kinetoflow.backend.dto.PatientPlanDto;
import com.kinetoflow.backend.dto.PatientPlanServiceItemDto;
import com.kinetoflow.backend.dto.ServiceDto;
import com.kinetoflow.backend.dto.UpdatePlanItemQuantitiesRequest;
import com.kinetoflow.backend.dto.UpdatePlanItemStatusRequest;
import com.kinetoflow.backend.dto.UpdatePlanStatusRequest;
import com.kinetoflow.backend.dto.UserSummaryDto;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.service.AppointmentService;
import com.kinetoflow.backend.service.CalendarService;
import com.kinetoflow.backend.service.CompanyAdminServiceManagementService;
import com.kinetoflow.backend.service.InvitationService;
import com.kinetoflow.backend.service.MedicWorkingHoursService;
import com.kinetoflow.backend.service.PatientPlanService;
import com.kinetoflow.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/medic")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MEDIC')")
public class MedicController {

    private final UserService userService;
    private final CompanyAdminServiceManagementService managementService;
    private final CalendarService calendarService;
    private final PatientPlanService patientPlanService;
    private final InvitationService invitationService;
    private final AppointmentService appointmentService;
    private final MedicWorkingHoursService medicWorkingHoursService;

    // --- Patient Listing & Invites (Existing) ---
    @GetMapping("/my-patients")
    public ResponseEntity<List<UserSummaryDto>> getMyAssignedPatients(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getAssignedActivePatients(currentUser));
    }

    @GetMapping("/my-pending-invites")
    public ResponseEntity<List<UserSummaryDto>> getMyPendingInvites(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getAssignedPendingPatients(currentUser));
    }

    @PostMapping("/invites/resend")
    public ResponseEntity<Void> resendPatientInvitation(@AuthenticationPrincipal User currentUser, @Valid @RequestBody InvitationRequest request) {
        invitationService.sendInvitation(request);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/invites/{pendingUserId}/cancel")
    public ResponseEntity<Void> cancelPatientInvitation(@AuthenticationPrincipal User currentUser, @PathVariable Long pendingUserId) {
        userService.cancelPatientInvitation(currentUser, pendingUserId);
        return ResponseEntity.noContent().build();
    }

    // --- Service & Package Viewing (Existing) ---
    @GetMapping("/company-services")
    public ResponseEntity<List<ServiceDto>> getActiveCompanyServices(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getCompany() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(managementService.getActiveServicesForCompany(currentUser.getCompany()));
    }

    @GetMapping("/company-packages")
    public ResponseEntity<List<PackageDto>> getActiveCompanyPackages(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getCompany() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(managementService.getActivePackagesForCompany(currentUser.getCompany()));
    }

    // --- Patient Plan Management (Existing) ---
    @PostMapping("/patients/{patientId}/plans")
    public ResponseEntity<PatientPlanDto> assignPlanToPatient(@AuthenticationPrincipal User currentUser, @PathVariable Long patientId, @Valid @RequestBody AssignPlanRequestDto request) {
        PatientPlanDto c = patientPlanService.assignPlanToPatient(currentUser, patientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(c);
    }

    @GetMapping("/patients/{patientId}/plans")
    public ResponseEntity<List<PatientPlanDto>> getPatientPlans(@AuthenticationPrincipal User currentUser, @PathVariable Long patientId) {
        List<PatientPlanDto> p = patientPlanService.getPlansForPatient(currentUser, patientId);
        return ResponseEntity.ok(p);
    }

    @PatchMapping("/patient-plans/{planId}/status")
    public ResponseEntity<Void> updatePatientPlanStatus(@AuthenticationPrincipal User currentUser, @PathVariable Long planId, @Valid @RequestBody UpdatePlanStatusRequest request) {
        patientPlanService.updatePatientPlanStatusByMedic(currentUser, planId, request.getIsActive());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/patient-plan-items/{planItemId}/status")
    public ResponseEntity<Void> updatePlanItemStatus(@AuthenticationPrincipal User currentUser, @PathVariable Long planItemId, @Valid @RequestBody UpdatePlanItemStatusRequest request) {
        patientPlanService.updatePatientPlanItemStatusByMedic(currentUser, planItemId, request.getIsItemActive());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/patient-plans/{planId}")
    public ResponseEntity<Void> archivePatientPlan(@AuthenticationPrincipal User currentUser, @PathVariable Long planId) {
        patientPlanService.archivePatientPlanByMedic(currentUser, planId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/patient-plan-items/{planItemId}")
    public ResponseEntity<Void> archivePatientPlanItem(@AuthenticationPrincipal User currentUser, @PathVariable Long planItemId) {
        patientPlanService.archivePatientPlanItemByMedic(currentUser, planItemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/patient-plan-items/{planItemId}/quantities")
    public ResponseEntity<PatientPlanServiceItemDto> updatePlanItemQuantities(@AuthenticationPrincipal User currentUser, @PathVariable Long planItemId, @Valid @RequestBody UpdatePlanItemQuantitiesRequest request) {
        PatientPlanServiceItemDto u = patientPlanService.updatePatientPlanItemQuantitiesByMedic(currentUser, planItemId, request);
        return ResponseEntity.ok(u);
    }

    // --- Calendar/Scheduling ---
    @GetMapping("/calendar/events")
    public ResponseEntity<List<CalendarEventDto>> getCalendarEvents(@AuthenticationPrincipal User currentUser, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<CalendarEventDto> e = calendarService.getMedicCalendarEvents(currentUser, start, end);
        return ResponseEntity.ok(e);
    }

    @PostMapping("/time-blocks")
    public ResponseEntity<CalendarEventDto> createTimeBlock(@AuthenticationPrincipal User currentUser, @Valid @RequestBody CreateTimeBlockRequest request) {
        CalendarEventDto c = calendarService.createTimeBlock(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(c);
    }

    @DeleteMapping("/time-blocks/{timeBlockId}")
    public ResponseEntity<Void> deleteTimeBlock(@AuthenticationPrincipal User currentUser, @PathVariable Long timeBlockId) {
        calendarService.deleteTimeBlock(currentUser, timeBlockId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentDto> createAppointment(@AuthenticationPrincipal User currentUser, @Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentDto c = appointmentService.createAppointment(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(c);
    }
    
    @PatchMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<AppointmentDto> cancelAppointment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request) {
        log.info("Medic {} requesting to cancel appointment ID {} with status {}", currentUser.getEmail(), appointmentId, request.getNewStatus());
        AppointmentDto cancelledAppointment = appointmentService.cancelAppointmentByMedic(currentUser, appointmentId, request);
        return ResponseEntity.ok(cancelledAppointment);
    }

    @DeleteMapping("/appointments/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(
            @AuthenticationPrincipal User medic,
            @PathVariable Long appointmentId) {
        log.info("Medic {} attempting to delete appointment {}", medic.getEmail(), appointmentId);
        appointmentService.deleteAppointment(medic, appointmentId);
        return ResponseEntity.noContent().build();
    }

    // --- Medic Working Hours ---    
    @GetMapping("/working-hours")
    @PreAuthorize("hasRole('MEDIC')")
    public ResponseEntity<List<MedicWorkingHoursDto>> getMyWorkingHours(@AuthenticationPrincipal User medic) {
        return ResponseEntity.ok(medicWorkingHoursService.getWorkingHours(medic));
    }

    @PostMapping("/working-hours")
    @PreAuthorize("hasRole('MEDIC')")
    public ResponseEntity<List<MedicWorkingHoursDto>> setMyWorkingHours(@AuthenticationPrincipal User medic, @Valid @RequestBody List<MedicWorkingHoursDto> workingHoursList) {
        // This endpoint expects a list to potentially replace all working hours.
        // The service method setMultipleWorkingHours can be adapted if it should clear existing first or upsert.
        // For now, it assumes an upsert logic per day based on the service implementation.
        return ResponseEntity.ok(medicWorkingHoursService.setMultipleWorkingHours(medic, workingHoursList));
    }

    // Example for setting/updating a single day - could be a PUT or POST to a specific day path
    @PutMapping("/working-hours/day") // Or @PostMapping if preferred for creation/update
    @PreAuthorize("hasRole('MEDIC')")
    public ResponseEntity<MedicWorkingHoursDto> setWorkingHoursForDay(@AuthenticationPrincipal User medic, @Valid @RequestBody MedicWorkingHoursDto workingHoursDto) {
        return ResponseEntity.ok(medicWorkingHoursService.setWorkingHours(medic, workingHoursDto));
    }

    @DeleteMapping("/working-hours/{dayOfWeek}")
    @PreAuthorize("hasRole('MEDIC')")
    public ResponseEntity<Void> deleteWorkingHoursForDay(@AuthenticationPrincipal User medic, @PathVariable DayOfWeek dayOfWeek) {
        medicWorkingHoursService.deleteWorkingHoursForDay(medic, dayOfWeek);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/working-hours/all")
    @PreAuthorize("hasRole('MEDIC')")
    public ResponseEntity<Void> clearAllMyWorkingHours(@AuthenticationPrincipal User medic) {
        medicWorkingHoursService.clearAllWorkingHours(medic);
        return ResponseEntity.noContent().build();
    }
}