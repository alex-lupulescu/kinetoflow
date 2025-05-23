package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.Package;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    // Fetch packages with their items and services eagerly to avoid N+1 queries in listings
    @EntityGraph(attributePaths = {"items", "items.service"}) // Load items and the service within each item
    List<Package> findByCompanyOrderByNameAsc(Company company);

    // Find a specific package by ID and Company, also fetching items+services
    @EntityGraph(attributePaths = {"items", "items.service"})
    Optional<Package> findByIdAndCompany(Long id, Company company);

    // Check if a package with the same name exists within a company
    boolean existsByNameAndCompany(String name, Company company);

    // Check if a package with the same name and different ID exists (for updates)
    boolean existsByNameAndCompanyAndIdNot(String name, Company company, Long id);

    // Future: Add query to check if package is assigned to any active PatientPlan
    // @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN TRUE ELSE FALSE END " +
    //        "FROM PatientPlan pp WHERE pp.originatingPackage.id = :packageId AND pp.isActive = true")
    // boolean isAssignedToActivePatientPlans(@Param("packageId") Long packageId);

    List<Package> findByCompanyAndIsActiveOrderByNameAsc(Company company, boolean isActive);

}