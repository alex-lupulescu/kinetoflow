package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.AcceptInvitationRequest;
import com.kinetoflow.backend.dto.InvitationDetailsResponse;
import com.kinetoflow.backend.dto.InvitationRequest;
import com.kinetoflow.backend.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * Endpoint for Admins (APP or COMPANY) or Medics to send an invitation.
     * Requires authentication and appropriate role.
     * Service layer enforces *who* can invite *whom*.
     */
    @PostMapping("/send")
    // <<< --- MODIFIED: Added ROLE_MEDIC --- >>>
    @PreAuthorize("hasRole('APP_ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('MEDIC')")
    public ResponseEntity<Void> sendInvitation(@Valid @RequestBody InvitationRequest invitationRequest) {
        // Note: The InvitationRequest DTO itself doesn't change, but the backend
        // service now interprets it based on the caller's role.
        // A Medic caller doesn't need to provide companyId (it's taken from their profile).
        // A Medic caller *must* set the role in the request to USER.
        log.info("Received request to send invitation: {}", invitationRequest);
        invitationService.sendInvitation(invitationRequest);
        return ResponseEntity.accepted().build(); // 202 Accepted
    }

    /**
     * Endpoint to retrieve details about an invitation using the token. Public.
     */
    @GetMapping("/details/{token}")
    public ResponseEntity<InvitationDetailsResponse> getInvitationDetails(@PathVariable String token) {
        log.info("Received request to get details for invitation token: {}", token);
        InvitationDetailsResponse details = invitationService.getInvitationDetails(token);
        return ResponseEntity.ok(details);
    }

    /**
     * Endpoint for a user to accept an invitation. Public.
     */
    @PostMapping("/accept")
    public ResponseEntity<Void> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest acceptRequest) {
        log.info("Received request to accept invitation for token: {}", acceptRequest.token());
        invitationService.acceptInvitation(acceptRequest);
        return ResponseEntity.ok().build(); // 200 OK
    }
}