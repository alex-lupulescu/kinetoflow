package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import Optional
import java.util.Set;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Find services by company, ordered by name
    List<Service> findByCompanyOrderByNameAsc(Company company);

    // Find active services by company, ordered by name
    List<Service> findByCompanyAndIsActiveOrderByNameAsc(Company company, boolean isActive);

    // Find a specific service by ID and Company (for security checks)
    Optional<Service> findByIdAndCompany(Long id, Company company);

    // Check if a service with the same name exists within a company
    boolean existsByNameAndCompany(String name, Company company);

    // Check if a service with the same name and different ID exists (for updates)
    boolean existsByNameAndCompanyAndIdNot(String name, Company company, Long id);

    // Find services by their IDs and company (useful for validating services added to packages)
    List<Service> findByIdInAndCompany(Set<Long> ids, Company company);

    // Check if a service is part of any active package
    @Query("SELECT CASE WHEN COUNT(psi) > 0 THEN TRUE ELSE FALSE END " +
            "FROM PackageServiceItem psi JOIN psi.pack p " + // Use 'pack' field name
            "WHERE psi.service.id = :serviceId AND p.isActive = true")
    boolean isServiceInActivePackage(@Param("serviceId") Long serviceId); // Use @Param
}