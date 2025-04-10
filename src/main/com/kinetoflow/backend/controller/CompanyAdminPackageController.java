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
 * Controller for Company Admins to manage Packages within their company.
 */
@RestController
@RequestMapping("/api/company-admin/packages") // Base path for package management
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('COMPANY_ADMIN')") // Secure all endpoints
public class CompanyAdminPackageController {

    private final CompanyAdminServiceManagementService managementService;

    @PostMapping
    public ResponseEntity<PackageDto> createPackage(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreatePackageRequest request) {
        log.info("Request from {} to create package '{}'", currentUser.getEmail(), request.name());
        PackageDto createdPackage = managementService.createPackage(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPackage);
    }

    @GetMapping
    public ResponseEntity<List<PackageDto>> getPackages(@AuthenticationPrincipal User currentUser) {
        log.info("Request from {} to get packages", currentUser.getEmail());
        List<PackageDto> packages = managementService.getPackages(currentUser);
        return ResponseEntity.ok(packages);
    }

    @PutMapping("/{packageId}")
    public ResponseEntity<PackageDto> updatePackage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long packageId,
            @Valid @RequestBody UpdatePackageRequest request) {
        log.info("Request from {} to update package ID {}", currentUser.getEmail(), packageId);
        PackageDto updatedPackage = managementService.updatePackage(currentUser, packageId, request);
        return ResponseEntity.ok(updatedPackage);
    }

    @PatchMapping("/{packageId}/status")
    public ResponseEntity<Void> updatePackageStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long packageId,
            @Valid @RequestBody UpdateActiveStatusRequest request) { // Use generic DTO
        log.info("Request from {} to update status for package ID {} to {}", currentUser.getEmail(), packageId, request.getIsActive());
        managementService.updatePackageStatus(currentUser, packageId, request.getIsActive());
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // Add DELETE endpoint later if needed
    // @DeleteMapping("/{packageId}")
    // public ResponseEntity<Void> deletePackage(...) { ... }
}