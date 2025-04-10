package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.Company; // Import Company
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.enums.UserRole; // Import UserRole
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Import List
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email (used for login and checking duplicates)
    Optional<User> findByEmail(String email);

    // Find user by invitation token (used for accepting invitations)
    Optional<User> findByInvitationToken(String token);

    // Check if an email already exists
    boolean existsByEmail(String email);

    // Find users by company and role (for Company Admin listings)
    List<User> findByCompanyAndRole(Company company, UserRole role);

    // Find users by company, role, and active status
    List<User> findByCompanyAndRoleAndIsActive(Company company, UserRole role, boolean isActive);

    // Find patients (role=USER) assigned to a specific medic within a company
    List<User> findByCompanyAndRoleAndAssignedMedic(Company company, UserRole role, User medic);

    // --- Methods for Dashboard Stats ---
    long countByCompanyAndRoleAndIsActive(Company company, UserRole role, boolean isActive);
    long countByCompanyAndRoleAndIsActiveAndAssignedMedicIsNull(Company company, UserRole role, boolean isActive); // Count active, unassigned patients

    List<User> findByAssignedMedicAndIsActiveTrueOrderByNameAsc(User medic);

    List<User> findByAssignedMedicAndIsActiveFalseAndInvitationTokenIsNotNullOrderByNameAsc(User medic);
}