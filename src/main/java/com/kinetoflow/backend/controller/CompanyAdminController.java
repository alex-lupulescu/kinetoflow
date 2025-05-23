package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.AssignPatientRequest;
import com.kinetoflow.backend.dto.UpdateUserStatusRequest;
import com.kinetoflow.backend.dto.UserSummaryDto;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/company-admin") // Base path for company admin specific actions
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('COMPANY_ADMIN')") // Secure all methods in this controller
public class CompanyAdminController {

    private final UserService userService;

    // --- Medic Management ---

    @GetMapping("/medics")
    public ResponseEntity<List<UserSummaryDto>> getMedics(@AuthenticationPrincipal User currentUser) {
        log.info("Company Admin {} requesting medic list for company {}", currentUser.getEmail(), currentUser.getCompany().getId());
        List<UserSummaryDto> medics = userService.getUsersByCompanyAndRole(currentUser.getCompany(), UserRole.MEDIC);
        return ResponseEntity.ok(medics);
    }

    // Note: Inviting medics uses the existing POST /api/invitations/send endpoint,
    // the logic in InvitationService handles the COMPANY_ADMIN context.


    // --- Patient Management ---

    @GetMapping("/patients")
    public ResponseEntity<List<UserSummaryDto>> getPatients(@AuthenticationPrincipal User currentUser) {
        log.info("Company Admin {} requesting patient list for company {}", currentUser.getEmail(), currentUser.getCompany().getId());
        List<UserSummaryDto> patients = userService.getUsersByCompanyAndRole(currentUser.getCompany(), UserRole.USER);
        return ResponseEntity.ok(patients);
    }

    @PatchMapping("/patients/{patientId}/assign-medic")
    public ResponseEntity<UserSummaryDto> assignMedic(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long patientId,
            @Valid @RequestBody AssignPatientRequest request) { // medicId in request body
        log.info("Company Admin {} assigning medic ID {} to patient ID {}", currentUser.getEmail(), request.getMedicId(), patientId);
        UserSummaryDto updatedPatient = userService.assignMedicToPatient(currentUser.getCompany(), patientId, request.getMedicId());
        return ResponseEntity.ok(updatedPatient);
    }


    // --- Common User Status Update ---

    @PatchMapping("/users/{userId}/status") // Applies to both Medics and Patients
    public ResponseEntity<UserSummaryDto> updateUserStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        log.info("Company Admin {} updating status for user ID {} to {}", currentUser.getEmail(), userId, request.getIsActive());
        UserSummaryDto updatedUser = userService.updateUserStatus(currentUser.getCompany(), userId, request.getIsActive());
        return ResponseEntity.ok(updatedUser);
    }
}