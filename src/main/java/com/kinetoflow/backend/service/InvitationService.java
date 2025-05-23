package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.AcceptInvitationRequest;
import com.kinetoflow.backend.dto.InvitationDetailsResponse;
import com.kinetoflow.backend.dto.InvitationRequest;
import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.exception.ResourceNotFoundException;
import com.kinetoflow.backend.repository.CompanyRepository;
import com.kinetoflow.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// No UsernameNotFoundException needed here
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.invitation.token.expiration.minutes}")
    private long tokenExpirationMinutes;

    @Transactional
    public void sendInvitation(InvitationRequest request) {
        log.info("Processing invitation request for email: {}", request.email());

        // --- Get Inviter ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new BadRequestException("Inviter authentication details not found.");
        }
        User inviter = (User) authentication.getPrincipal();
        log.debug("Invitation initiated by user: {} ({}) with role: {}", inviter.getEmail(), inviter.getId(), inviter.getRole());

        // --- Permission & Validation ---
        if (request.role() == null) {
            throw new BadRequestException("Invitee role must be specified.");
        }
        if (request.role() == UserRole.APP_ADMIN) {
            throw new BadRequestException("APP_ADMIN role cannot be assigned via invitation.");
        }
        // <<< --- MODIFICATION START --- >>>
        // Medics can ONLY invite Patients (Users)
        if (inviter.getRole() == UserRole.MEDIC && request.role() != UserRole.USER) {
            throw new BadRequestException("Medics can only invite Patients (Users).");
        }
        // Admins (App or Company) cannot invite Patients directly via this flow (can be changed if needed)
        if ((inviter.getRole() == UserRole.APP_ADMIN || inviter.getRole() == UserRole.COMPANY_ADMIN) && request.role() == UserRole.USER) {
            throw new BadRequestException("Administrators cannot directly invite Patients using this function. Patients should be invited by Medics.");
            // Alternative: Allow admins to invite patients but require assigning a medic later.
        }
        // <<< --- MODIFICATION END --- >>>

        // Validate permissions and get company (extracted helper)
        Company companyForInvite = validatePermissionsAndGetCompany(request, inviter);

        // Ensure company is determined (should be for Medic/CompanyAdmin invites to Medic/User)
        if (companyForInvite == null && request.role() != UserRole.COMPANY_ADMIN) {
            // This might happen if an APP_ADMIN tries to invite MEDIC/USER without companyId
            // The helper might already catch this, but double-check.
            throw new BadRequestException("Company association is required for this invitation role.");
        }

        // --- Ensure Company is attached for email name retrieval ---
        Company attachedCompany = null;
        if (companyForInvite != null) {
            attachedCompany = companyRepository.findById(companyForInvite.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Associated company not found with ID: " + companyForInvite.getId()));
        }

        // --- User Handling ---
        Optional<User> existingUserOpt = userRepository.findByEmail(request.email());
        User userToInvite;

        if (existingUserOpt.isPresent()) {
            // ... (logic for reusing existing inactive user - unchanged) ...
            User existingUser = existingUserOpt.get();
            if (existingUser.isActive()) {
                throw new BadRequestException("An active user with this email already exists.");
            }
            if (existingUser.getInvitationToken() != null && existingUser.getInvitationTokenExpiryDate() != null && existingUser.getInvitationTokenExpiryDate().isAfter(LocalDateTime.now())) {
                log.warn("User {} already has a pending invitation. Overwriting.", request.email());
            }
            userToInvite = existingUser;
            log.info("Found existing inactive user with email {}. Reusing record.", request.email());
        } else {
            // Create a new user record
            userToInvite = new User();
            userToInvite.setEmail(request.email());
            userToInvite.setName("Invited: " + request.email()); // Placeholder name
            userToInvite.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Placeholder password
            log.info("Creating new user record for invitation: {}", request.email());
        }

        // --- Update User with Invitation Details ---
        userToInvite.setRole(request.role());
        userToInvite.setCompany(attachedCompany); // Assign company
        userToInvite.setActive(false); // Ensure inactive
        // <<< --- MODIFICATION START: Assign Medic if Inviter is Medic --- >>>
        if (inviter.getRole() == UserRole.MEDIC && request.role() == UserRole.USER) {
            userToInvite.setAssignedMedic(inviter); // Automatically assign the inviting medic
            log.info("Assigning inviting medic {} to patient {}", inviter.getId(), request.email());
        } else {
            // Ensure medic is null if invited by admin or if inviting non-patient
            userToInvite.setAssignedMedic(null);
        }
        // <<< --- MODIFICATION END --- >>>

        String token = generateUniqueToken();
        userToInvite.setInvitationToken(token);
        userToInvite.setInvitationTokenExpiryDate(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));

        userRepository.save(userToInvite);
        log.info("Invitation token generated and user {} saved/updated for email: {}", userToInvite.getId(), request.email());

        // --- Send Email ---
        String invitationLink = frontendUrl + "/accept-invitation/" + token;
        String companyNameForEmail = (attachedCompany != null) ? attachedCompany.getName() : "KinetoFlow Platform";
        // Use inviter's name from the authenticated principal
        String inviterDisplayName = inviter.getName();

        emailService.sendInvitationEmail(
                request.email(),
                inviterDisplayName,
                companyNameForEmail,
                request.role().name().replace("_", " "),
                invitationLink
        );
    }

    /**
     * Retrieves the details needed for the frontend 'accept invitation' page based on the token.
     *
     * @param token The invitation token.
     * @return DTO containing relevant invitation details.
     * @throws ResourceNotFoundException If the token is not found or invalid.
     * @throws BadRequestException       If the invitation is expired or already accepted.
     */
    @Transactional(readOnly = true)
    public InvitationDetailsResponse getInvitationDetails(String token) {
        log.debug("Fetching details for invitation token: {}", token);
        User user = findValidInviteeByToken(token);

        return InvitationDetailsResponse.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
                .inviterName(null) // Placeholder - requires 'invitedBy' field on User entity
                .build();
    }

    /**
     * Activates a user account based on a valid invitation token and user-provided details.
     *
     * @param request DTO containing the token, name, and password.
     * @throws ResourceNotFoundException If the token is not found or invalid.
     * @throws BadRequestException       If the invitation is expired, already accepted, or password invalid.
     */
    @Transactional
    public void acceptInvitation(AcceptInvitationRequest request) {
        log.info("Accepting invitation with token: {}", request.token());

        if (request.password() == null || request.password().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long.");
        }

        User user = findValidInviteeByToken(request.token());

        // Update user details
        user.setName(request.name()); // Set the actual name provided by the user
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setInvitationToken(null);
        user.setInvitationTokenExpiryDate(null);

        userRepository.save(user);
        log.info("User {} ({}) successfully activated via invitation.", user.getEmail(), user.getId());

        // Optional: Send a welcome email after activation
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
    }

    // --- Helper Methods ---

    /**
     * Finds a user by a potentially valid invitation token and checks its validity.
     * Ensures user exists, token matches, user is inactive, and token hasn't expired.
     *
     * @param token The invitation token.
     * @return The User entity if valid.
     * @throws ResourceNotFoundException If token not found.
     * @throws BadRequestException       If user already active or token expired.
     */
    private User findValidInviteeByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Invitation token cannot be empty.");
        }
        User user = userRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation token not found or invalid."));

        if (user.isActive()) {
            throw new BadRequestException("This invitation has already been accepted.");
        }

        if (user.getInvitationTokenExpiryDate() != null && user.getInvitationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invitation token has expired.");
        }
        return user;
    }

    // validatePermissionsAndGetCompany needs slight adjustment for Medic
    private Company validatePermissionsAndGetCompany(InvitationRequest request, User inviter) {
        Company company = null;
        UserRole inviterRole = inviter.getRole();
        UserRole targetRole = request.role(); // Role being invited

        switch (inviterRole) {
            case MEDIC: // <<< --- NEW CASE --- >>>
                company = inviter.getCompany();
                if (company == null) {
                    throw new BadRequestException("Medic must belong to a company to send invitations.");
                }
                // Medic can ONLY invite USERs to their own company
                if (targetRole != UserRole.USER) {
                    // This is already checked at the beginning of sendInvitation, but double check here
                    throw new BadRequestException("Medics can only invite Users.");
                }
                log.debug("MEDIC {} inviting {} to company {}", inviter.getId(), targetRole, company.getId());
                break;
            // <<< --- END NEW CASE --- >>>

            case COMPANY_ADMIN:
                company = inviter.getCompany();
                if (company == null) { /* ... error ... */ }
                // Company admin can only invite MEDIC or USER (as per previous logic)
                if (targetRole != UserRole.MEDIC && targetRole != UserRole.USER) {
                    throw new BadRequestException("Company Admin can only invite Medics or Users.");
                }
                log.debug("COMPANY_ADMIN {} inviting {} to company {}", inviter.getId(), targetRole, company.getId());
                break;

            case APP_ADMIN:
                // APP_ADMIN can invite COMPANY_ADMIN or MEDIC (as per previous logic)
                // We explicitly disallowed inviting USER earlier for APP_ADMIN/COMPANY_ADMIN
                if (targetRole == UserRole.COMPANY_ADMIN) {
                    if (request.companyId() != null) { /* ... find company ... */ } else { /* ... log invite without company ... */ }
                } else if (targetRole == UserRole.MEDIC) { // Only MEDIC here now
                    if (request.companyId() == null) {
                        throw new BadRequestException("Company ID is required when APP_ADMIN invites a Medic.");
                    }
                    company = companyRepository.findById(request.companyId())
                            .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + request.companyId()));
                    log.debug("APP_ADMIN inviting {} to company {}", targetRole, company.getId());
                } else {
                    // Should be caught earlier, but safe fallback
                    throw new BadRequestException("APP_ADMIN cannot invite role: " + targetRole);
                }
                break;

            default: // USER cannot invite
                log.warn("Unauthorized invitation attempt by user {} with role {}", inviter.getEmail(), inviterRole);
                throw new BadRequestException("You do not have permission to send invitations.");
        }
        return company;
    }

    private String generateUniqueToken() {
        return UUID.randomUUID().toString();
    }
}