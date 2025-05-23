package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.UpdateProfileRequest;
import com.kinetoflow.backend.dto.UserSummaryDto;
import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.exception.ResourceNotFoundException;
import com.kinetoflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Import PasswordEncoder if handling password changes here
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    // Inject PasswordEncoder if needed for profile update
    // private final PasswordEncoder passwordEncoder;

    /**
     * Retrieves users belonging to a specific company based on their role.
     * Used by Company Admin to list Medics or Patients.
     *
     * @param company The company entity.
     * @param role    The role to filter by (MEDIC or USER).
     * @return List of UserSummaryDto.
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getUsersByCompanyAndRole(Company company, UserRole role) {
        if (company == null) {
            log.warn("Attempted to get users for a null company.");
            throw new BadRequestException("Company information is required.");
        }
        if (role != UserRole.MEDIC && role != UserRole.USER) {
            log.warn("Attempted to get users with invalid role filter: {}", role);
            throw new BadRequestException("Can only list Medics or Users for a company.");
        }
        log.debug("Fetching users for company {} with role {}", company.getId(), role);
        List<User> users = userRepository.findByCompanyAndRole(company, role);
        return users.stream().map(UserSummaryDto::fromEntity).collect(Collectors.toList());
    }

    /**
     * Updates the active status of a user within a specific company.
     * Used by Company Admin to activate/deactivate Medics or Patients.
     *
     * @param company The company of the admin performing the action.
     * @param userId  The ID of the user whose status is being updated.
     * @param isActive The new active status.
     * @return The updated UserSummaryDto.
     * @throws ResourceNotFoundException If the user is not found.
     * @throws BadRequestException       If the user doesn't belong to the admin's company or is an admin.
     */
    @Transactional
    public UserSummaryDto updateUserStatus(Company company, Long userId, boolean isActive) {
        if (company == null) {
            throw new BadRequestException("Company information is required.");
        }
        log.info("Attempting to update status for user ID {} to {} within company {}", userId, isActive, company.getId());

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Security Check: Ensure the user belongs to the admin's company
        if (userToUpdate.getCompany() == null || !userToUpdate.getCompany().getId().equals(company.getId())) {
            log.warn("Forbidden attempt: User {} does not belong to company {}", userId, company.getId());
            throw new BadRequestException("User does not belong to your company.");
        }

        // Prevent company admin from deactivating themselves or other admins
        if (userToUpdate.getRole() == UserRole.COMPANY_ADMIN || userToUpdate.getRole() == UserRole.APP_ADMIN) {
            log.warn("Attempt to change status of an admin user {} denied.", userId);
            throw new BadRequestException("Cannot change the status of an administrative user.");
        }

        // Prevent activating a user who hasn't accepted an invitation (if token still present)
        if (isActive && userToUpdate.getInvitationToken() != null) {
            log.warn("Attempt to activate user {} who still has a pending invitation.", userId);
            throw new BadRequestException("Cannot activate a user with a pending invitation. They must accept it first.");
        }

        userToUpdate.setActive(isActive);
        User updatedUser = userRepository.save(userToUpdate);
        log.info("Successfully updated status for user ID {} to {}", userId, isActive);

        return UserSummaryDto.fromEntity(updatedUser);
    }

    /**
     * Assigns or unassigns a Medic to a Patient within a specific company.
     *
     * @param company   The company of the admin performing the action.
     * @param patientId The ID of the patient (must have USER role).
     * @param medicId   The ID of the medic (must have MEDIC role), or null to unassign.
     * @return The updated patient's UserSummaryDto.
     * @throws ResourceNotFoundException If patient or medic (if provided) not found.
     * @throws BadRequestException       If users don't belong to the company, have incorrect roles, etc.
     */
    @Transactional
    public UserSummaryDto assignMedicToPatient(Company company, Long patientId, Long medicId) {
        if (company == null) {
            throw new BadRequestException("Company information is required.");
        }
        log.info("Attempting to assign medic ID {} to patient ID {} within company {}", medicId, patientId, company.getId());

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        // Validate Patient
        if (patient.getRole() != UserRole.USER) {
            throw new BadRequestException("User with ID " + patientId + " is not a patient.");
        }
        if (patient.getCompany() == null || !patient.getCompany().getId().equals(company.getId())) {
            throw new BadRequestException("Patient does not belong to your company.");
        }

        User medic = null;
        if (medicId != null) {
            medic = userRepository.findById(medicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Medic not found with ID: " + medicId));
            // Validate Medic
            if (medic.getRole() != UserRole.MEDIC) {
                throw new BadRequestException("User with ID " + medicId + " is not a medic.");
            }
            if (medic.getCompany() == null || !medic.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Medic does not belong to your company.");
            }
            if (!medic.isActive()) {
                throw new BadRequestException("Cannot assign an inactive medic.");
            }
        }

        patient.setAssignedMedic(medic); // Assign or unassign (if medic is null)
        User updatedPatient = userRepository.save(patient);
        log.info("Successfully updated assigned medic for patient {}", patientId);

        return UserSummaryDto.fromEntity(updatedPatient);
    }


    /**
     * Updates the profile information (currently just name) for the currently authenticated user.
     *
     * @param currentUser The currently authenticated user entity.
     * @param request     DTO containing the updated profile data.
     * @return The updated UserSummaryDto for the current user.
     */
    @Transactional
    public UserSummaryDto updateCurrentUserProfile(User currentUser, UpdateProfileRequest request) {
        log.info("Updating profile for user {}", currentUser.getEmail());
        currentUser.setName(request.getName());
        // Add password change logic here if implementing password update
        // if (request.getPassword() != null && !request.getPassword().isBlank()) {
        //    // Add validation for old password if required
        //    currentUser.setPassword(passwordEncoder.encode(request.getPassword()));
        // }
        User updatedUser = userRepository.save(currentUser);
        log.info("Profile updated successfully for user {}", currentUser.getEmail());
        return UserSummaryDto.fromEntity(updatedUser);
    }

    /**
     * Retrieves active patients assigned to a specific medic.
     *
     * @param medic The medic user entity.
     * @return List of UserSummaryDto for assigned active patients.
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAssignedActivePatients(User medic) {
        if (medic == null || medic.getRole() != UserRole.MEDIC) {
            throw new BadRequestException("Invalid medic user provided.");
        }
        log.debug("Fetching active patients assigned to medic {}", medic.getEmail());
        List<User> patients = userRepository.findByAssignedMedicAndIsActiveTrueOrderByNameAsc(medic);
        return patients.stream().map(UserSummaryDto::fromEntity).collect(Collectors.toList());
    }

    /**
     * Retrieves PENDING (inactive with token) patients assigned to a specific medic.
     *
     * @param medic The medic user entity.
     * @return List of UserSummaryDto for assigned pending patients.
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAssignedPendingPatients(User medic) {
        if (medic == null || medic.getRole() != UserRole.MEDIC) {
            throw new BadRequestException("Invalid medic user provided.");
        }
        log.debug("Fetching pending patients assigned to medic {}", medic.getEmail());
        // We need a new repository method for this specific query
        List<User> patients = userRepository.findByAssignedMedicAndIsActiveFalseAndInvitationTokenIsNotNullOrderByNameAsc(medic);
        return patients.stream().map(UserSummaryDto::fromEntity).collect(Collectors.toList());
    }

    /**
     * Cancels a pending invitation for a user.
     * Ensures the user belongs to the medic's company and is indeed pending.
     *
     * @param medic The medic user performing the action.
     * @param pendingUserId The ID of the user whose invitation is to be cancelled.
     * @throws ResourceNotFoundException If user not found.
     * @throws BadRequestException If user doesn't belong to company, is active, or has no pending invite.
     */
    @Transactional
    public void cancelPatientInvitation(User medic, Long pendingUserId) {
        Company company = medic.getCompany();
        if (company == null) {
            throw new BadRequestException("Medic must belong to a company.");
        }
        log.info("Medic {} attempting to cancel invitation for user ID {}", medic.getEmail(), pendingUserId);

        User userToCancel = userRepository.findById(pendingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + pendingUserId));

        // Security & Validation Checks
        if (userToCancel.getCompany() == null || !userToCancel.getCompany().getId().equals(company.getId())) {
            throw new BadRequestException("User does not belong to your company.");
        }
        if (userToCancel.getRole() != UserRole.USER) {
            throw new BadRequestException("Can only cancel patient invitations.");
        }
        if (userToCancel.isActive()) {
            throw new BadRequestException("Cannot cancel invitation: User is already active.");
        }
        if (userToCancel.getInvitationToken() == null) {
            throw new BadRequestException("Cannot cancel invitation: No pending invitation found for this user.");
        }
        // Optional: Check if the *medic* actually invited them or if they are assigned?
        // For now, allow any medic in the company to cancel a pending patient invite for that company.

        // Clear invitation fields
        userToCancel.setInvitationToken(null);
        userToCancel.setInvitationTokenExpiryDate(null);
        // Keep user record as inactive, but without an invite. Admin could potentially re-invite later.
        // Alternatively, delete the user record if desired: userRepository.delete(userToCancel);
        userRepository.save(userToCancel);
        log.info("Invitation cancelled successfully for user ID {}", pendingUserId);

        // Optional: Send a notification email to the user? Maybe not necessary.
    }
}