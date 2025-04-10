package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.*;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.service.CalendarService;
import com.kinetoflow.backend.service.CompanyAdminServiceManagementService;
import com.kinetoflow.backend.service.InvitationService;
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
import java.util.List;

@RestController
@RequestMapping("/api/medic")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MEDIC')")
public class MedicController {

    // Injected Services (Keep all needed)
    private final UserService userService;
    private final CompanyAdminServiceManagementService managementService;
    private final CalendarService calendarService;
    private final PatientPlanService patientPlanService;
    private final InvitationService invitationService;

    // === Patient Listing & Invites (Unchanged) ===
    @GetMapping("/my-patients")
    public ResponseEntity<List<UserSummaryDto>> getMyAssignedPatients(@AuthenticationPrincipal User currentUser) {
        log.info("Medic {} requesting assigned active patient list", currentUser.getEmail());
        List<UserSummaryDto> patients = userService.getAssignedActivePatients(currentUser);
        return ResponseEntity.ok(patients);
    }
    @GetMapping("/my-pending-invites")
    public ResponseEntity<List<UserSummaryDto>> getMyPendingInvites(@AuthenticationPrincipal User currentUser) {
        log.info("Medic {} requesting assigned pending patient invites", currentUser.getEmail());
        List<UserSummaryDto> pendingPatients = userService.getAssignedPendingPatients(currentUser);
        return ResponseEntity.ok(pendingPatients);
    }
    @PostMapping("/invites/resend")
    public ResponseEntity<Void> resendPatientInvitation(@AuthenticationPrincipal User currentUser, @Valid @RequestBody InvitationRequest request) {
        if (request.role() != UserRole.USER || request.email() == null || request.email().isBlank()) { throw new BadRequestException("Requires email and role 'USER'."); }
        log.info("Medic {} resending patient invitation to {}", currentUser.getEmail(), request.email());
        invitationService.sendInvitation(request);
        return ResponseEntity.accepted().build();
    }
    @DeleteMapping("/invites/{pendingUserId}/cancel") // Keep DELETE for cancelling invite
    public ResponseEntity<Void> cancelPatientInvitation(@AuthenticationPrincipal User currentUser, @PathVariable Long pendingUserId) {
        log.info("Medic {} cancelling patient invitation for user ID {}", currentUser.getEmail(), pendingUserId);
        userService.cancelPatientInvitation(currentUser, pendingUserId);
        return ResponseEntity.noContent().build();
    }

    // === Service & Package Viewing (Unchanged) ===
    @GetMapping("/company-services")
    public ResponseEntity<List<ServiceDto>> getActiveCompanyServices(@AuthenticationPrincipal User currentUser) {
        log.info("Medic {} requesting active company services", currentUser.getEmail());
        if (currentUser.getCompany() == null) { return ResponseEntity.badRequest().body(null); } // Return null body on bad request
        List<ServiceDto> services = managementService.getActiveServicesForCompany(currentUser.getCompany());
        return ResponseEntity.ok(services);
    }
    @GetMapping("/company-packages")
    public ResponseEntity<List<PackageDto>> getActiveCompanyPackages(@AuthenticationPrincipal User currentUser) {
        log.info("Medic {} requesting active company packages", currentUser.getEmail());
        if (currentUser.getCompany() == null) { return ResponseEntity.badRequest().body(null); }
        List<PackageDto> packages = managementService.getActivePackagesForCompany(currentUser.getCompany());
        return ResponseEntity.ok(packages);
    }

    // === Patient Plan Management (Simplified) ===
    @PostMapping("/patients/{patientId}/plans")
    public ResponseEntity<PatientPlanDto> assignPlanToPatient(@AuthenticationPrincipal User currentUser, @PathVariable Long patientId, @Valid @RequestBody AssignPlanRequestDto request) {
        log.info("Medic {} assigning plan to patient {}", currentUser.getEmail(), patientId);
        PatientPlanDto createdPlan = patientPlanService.assignPlanToPatient(currentUser, patientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
    }
    @GetMapping("/patients/{patientId}/plans")
    public ResponseEntity<List<PatientPlanDto>> getPatientPlans(@AuthenticationPrincipal User currentUser, @PathVariable Long patientId) {
        log.info("Medic {} requesting plans for patient {}", currentUser.getEmail(), patientId);
        List<PatientPlanDto> plans = patientPlanService.getPlansForPatient(currentUser, patientId);
        return ResponseEntity.ok(plans);
    }

    // --- KEPT: PATCH Endpoints for Activate/Deactivate ---
    @PatchMapping("/patient-plans/{planId}/status")
    public ResponseEntity<Void> updatePatientPlanStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long planId,
            @Valid @RequestBody UpdatePlanStatusRequest request) { // Uses {isActive: boolean} DTO
        log.info("Medic {} updating plan status for plan ID {} to {}", currentUser.getEmail(), planId, request.getIsActive());
        patientPlanService.updatePatientPlanStatusByMedic(currentUser, planId, request.getIsActive());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/patient-plan-items/{planItemId}/status")
    public ResponseEntity<Void> updatePlanItemStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long planItemId,
            @Valid @RequestBody UpdatePlanItemStatusRequest request) { // Uses {isItemActive: boolean} DTO
        log.info("Medic {} updating item status for item ID {} to {}", currentUser.getEmail(), planItemId, request.getIsItemActive());
        patientPlanService.updatePatientPlanItemStatusByMedic(currentUser, planItemId, request.getIsItemActive());
        return ResponseEntity.noContent().build();
    }

    // --- Archive (Soft Delete using DELETE method mapped to inactivation) ---
    @DeleteMapping("/patient-plans/{planId}") // Archive Plan
    public ResponseEntity<Void> archivePatientPlan(@AuthenticationPrincipal User currentUser, @PathVariable Long planId) {
        log.info("Medic {} ARCHIVING (soft deleting) plan ID {}", currentUser.getEmail(), planId);
        patientPlanService.archivePatientPlanByMedic(currentUser, planId);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/patient-plan-items/{planItemId}") // Archive Item
    public ResponseEntity<Void> archivePatientPlanItem(@AuthenticationPrincipal User currentUser, @PathVariable Long planItemId) {
        log.info("Medic {} ARCHIVING (soft deleting) plan item ID {}", currentUser.getEmail(), planItemId);
        patientPlanService.archivePatientPlanItemByMedic(currentUser, planItemId);
        return ResponseEntity.noContent().build();
    }


    // --- Quantity Correction Endpoint (Unchanged) ---
    @PutMapping("/patient-plan-items/{planItemId}/quantities")
    public ResponseEntity<PatientPlanServiceItemDto> updatePlanItemQuantities(@AuthenticationPrincipal User currentUser, @PathVariable Long planItemId, @Valid @RequestBody UpdatePlanItemQuantitiesRequest request) {
        log.info("Medic {} updating quantities for item ID {}", currentUser.getEmail(), planItemId);
        PatientPlanServiceItemDto updatedItem = patientPlanService.updatePatientPlanItemQuantitiesByMedic(currentUser, planItemId, request);
        return ResponseEntity.ok(updatedItem);
    }

    @PostMapping("/time-blocks") // Create a block (mark as unavailable)
    public ResponseEntity<CalendarEventDto> createTimeBlock(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateTimeBlockRequest request) {
        log.info("Medic {} creating time block", currentUser.getEmail());
        CalendarEventDto createdBlockDto = calendarService.createTimeBlock(currentUser, request);
        // Return 201 Created status
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBlockDto);
    }

    @DeleteMapping("/time-blocks/{timeBlockId}") // Delete a block (mark as available)
    public ResponseEntity<Void> deleteTimeBlock(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long timeBlockId) {
        log.info("Medic {} deleting time block {}", currentUser.getEmail(), timeBlockId);
        calendarService.deleteTimeBlock(currentUser, timeBlockId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/calendar/events")
    public ResponseEntity<List<CalendarEventDto>> getCalendarEvents(
            @AuthenticationPrincipal User currentUser, // Get the logged-in medic
            // Expect ISO date-time strings from FullCalendar (e.g., 2025-04-05T21:00:00.000Z)
            // Spring should parse these into LocalDateTime automatically with @RequestParam
            // Or use @DateTimeFormat if specific format needed, but ISO usually works.
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Medic {} requesting calendar events from {} to {}", currentUser.getEmail(), start, end);
        // Call the existing service method
        List<CalendarEventDto> events = calendarService.getMedicCalendarEvents(currentUser, start, end);
        return ResponseEntity.ok(events);
    }
}