package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.*;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.service.CompanyAdminServiceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Company Admins to manage Services within their company.
 */
@RestController
@RequestMapping("/api/company-admin/services") // Base path for service management
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('COMPANY_ADMIN')") // Secure all endpoints for Company Admin
public class CompanyAdminServiceController {

    private final CompanyAdminServiceManagementService managementService;

    @PostMapping
    public ResponseEntity<ServiceDto> createService(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateServiceRequest request) {
        log.info("Request from {} to create service '{}'", currentUser.getEmail(), request.name());
        ServiceDto createdService = managementService.createService(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
    }

    @GetMapping
    public ResponseEntity<List<ServiceDto>> getServices(@AuthenticationPrincipal User currentUser) {
        log.info("Request from {} to get services", currentUser.getEmail());
        List<ServiceDto> services = managementService.getServices(currentUser);
        return ResponseEntity.ok(services);
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceDto> updateService(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateServiceRequest request) {
        log.info("Request from {} to update service ID {}", currentUser.getEmail(), serviceId);
        ServiceDto updatedService = managementService.updateService(currentUser, serviceId, request);
        return ResponseEntity.ok(updatedService);
    }

    @PatchMapping("/{serviceId}/status")
    public ResponseEntity<Void> updateServiceStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateActiveStatusRequest request) { // Use generic DTO
        log.info("Request from {} to update status for service ID {} to {}", currentUser.getEmail(), serviceId, request.getIsActive());
        managementService.updateServiceStatus(currentUser, serviceId, request.getIsActive());
        return ResponseEntity.noContent().build(); // 204 No Content is suitable for status updates
    }

    // Add DELETE endpoint later if needed (with checks)
    // @DeleteMapping("/{serviceId}")
    // public ResponseEntity<Void> deleteService(...) { ... }
}