package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.CompanyDto;
import com.kinetoflow.backend.dto.CreateCompanyRequest;
import com.kinetoflow.backend.dto.UpdateCompanyRequest; // Import Update DTO
import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.User; // Import User
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.exception.ResourceNotFoundException; // Import ResourceNotFoundException
import com.kinetoflow.backend.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;

    // createCompany method remains the same...
    @Transactional
    public CompanyDto createCompany(CreateCompanyRequest request) {
        log.info("Attempting to create company with name: {}", request.name());
        if (companyRepository.existsByName(request.name())) {
            log.warn("Company creation failed: Company with name '{}' already exists.", request.name());
            throw new BadRequestException("Company with name '" + request.name() + "' already exists.");
        }
        Company company = new Company();
        company.setName(request.name());
        company.setAddress(request.address());
        Company savedCompany = companyRepository.save(company);
        log.info("Company created successfully with ID: {}", savedCompany.getId());
        return CompanyDto.fromEntity(savedCompany);
    }


    // getAllCompanies method remains the same...
    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies() {
        log.debug("Fetching all companies.");
        List<Company> companies = companyRepository.findAll();
        return companies.stream()
                .map(CompanyDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the details of the company associated with the given user (e.g., Company Admin).
     * Explicitly fetches the Company entity within the transaction to avoid LazyInitializationException.
     *
     * @param user The user whose company details are requested.
     * @return CompanyDto of the user's company.
     * @throws BadRequestException if the user is not associated with a company.
     * @throws ResourceNotFoundException if the associated company cannot be found (data integrity issue).
     */
    @Transactional(readOnly = true) // Transaction ensures session is active
    public CompanyDto getCompanyForUser(User user) {
        if (user == null) {
            log.warn("Attempted to get company details for a null user.");
            throw new BadRequestException("User information is required.");
        }
        // Get the company ID from the potentially proxied association
        Long companyId = (user.getCompany() != null) ? user.getCompany().getId() : null;

        if (companyId == null) {
            log.warn("User {} is not associated with any company.", user.getEmail());
            throw new BadRequestException("User is not associated with a company.");
        }

        log.debug("Fetching company details for company ID {} for user {}", companyId, user.getEmail());

        // --- FIX: Explicitly fetch the Company by ID within this transaction ---
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    // This case indicates a data integrity issue if the user has a companyId but the company doesn't exist
                    log.error("Data integrity issue: Company not found with ID {} associated with user {}", companyId, user.getEmail());
                    return new ResourceNotFoundException("Associated company not found with ID: " + companyId);
                });

        // Now 'company' is a fully managed entity within the current session.
        // Mapping to DTO should work without lazy loading issues.
        return CompanyDto.fromEntity(company);
    }



    /**
     * Updates the details (e.g., address) of the company associated with the given user.
     * Only allows updating the user's own company.
     *
     * @param user    The authenticated user (Company Admin) performing the update.
     * @param request DTO containing the fields to update.
     * @return CompanyDto of the updated company.
     * @throws BadRequestException if the user is not associated with a company.
     */
    @Transactional
    public CompanyDto updateCompanyForUser(User user, UpdateCompanyRequest request) {
        if (user == null || user.getCompany() == null) {
            log.warn("User {} cannot update company details as they are not associated with one.", user != null ? user.getEmail() : "null");
            throw new BadRequestException("User is not associated with a company.");
        }

        Company companyToUpdate = user.getCompany(); // Get the company directly from the authenticated user
        log.info("Updating company details for company ID: {}", companyToUpdate.getId());

        // Update allowed fields (currently only address)
        if (request.address() != null) {
            companyToUpdate.setAddress(request.address());
        }
        // If name updates were allowed:
        // if (request.name() != null && !request.name().isBlank()) {
        // Optional: Check if new name conflicts with another company
        // companyToUpdate.setName(request.name());
        // }

        Company updatedCompany = companyRepository.save(companyToUpdate);
        log.info("Company details updated successfully for company ID: {}", updatedCompany.getId());

        return CompanyDto.fromEntity(updatedCompany);
    }

    // Add deleteCompany later if needed (requires careful consideration of cascading effects)
}