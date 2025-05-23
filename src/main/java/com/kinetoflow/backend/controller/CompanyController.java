package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.CompanyDto;
import com.kinetoflow.backend.dto.CreateCompanyRequest;
import com.kinetoflow.backend.dto.UpdateCompanyRequest; // Import Update DTO
import com.kinetoflow.backend.entity.User; // Import User
import com.kinetoflow.backend.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Import
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    // POST /api/companies - Create (APP_ADMIN only) - Remains the same
    @PostMapping
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<CompanyDto> createCompany(@Valid @RequestBody CreateCompanyRequest createCompanyRequest) {
        log.info("Received request to create company: {}", createCompanyRequest.name());
        CompanyDto createdCompany = companyService.createCompany(createCompanyRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
    }

    // GET /api/companies - List All (APP_ADMIN only) - Remains the same
    @GetMapping
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<List<CompanyDto>> getAllCompanies() {
        log.info("Received request to get all companies");
        List<CompanyDto> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    // --- New Endpoints for Company Admin ---

    /**
     * Endpoint for a Company Admin to retrieve their own company's details.
     *
     * @param currentUser The currently authenticated user (injected).
     * @return ResponseEntity with CompanyDto and status 200 (OK).
     */
    @GetMapping("/my-company")
    @PreAuthorize("hasRole('COMPANY_ADMIN')") // Secure for Company Admin
    public ResponseEntity<CompanyDto> getMyCompany(@AuthenticationPrincipal User currentUser) {
        log.info("Company Admin {} requesting details for their company", currentUser.getEmail());
        CompanyDto companyDto = companyService.getCompanyForUser(currentUser);
        return ResponseEntity.ok(companyDto);
    }

    /**
     * Endpoint for a Company Admin to update their own company's details.
     *
     * @param currentUser The currently authenticated user (injected).
     * @param request     DTO containing the fields to update (e.g., address).
     * @return ResponseEntity with the updated CompanyDto and status 200 (OK).
     */
    @PutMapping("/my-company")
    @PreAuthorize("hasRole('COMPANY_ADMIN')") // Secure for Company Admin
    public ResponseEntity<CompanyDto> updateMyCompany(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateCompanyRequest request) {
        log.info("Company Admin {} updating details for their company", currentUser.getEmail());
        CompanyDto updatedCompany = companyService.updateCompanyForUser(currentUser, request);
        return ResponseEntity.ok(updatedCompany);
    }

    // Add GET /api/companies/{id} later if needed (likely APP_ADMIN only)
}