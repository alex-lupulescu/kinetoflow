package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.UpdateProfileRequest;
import com.kinetoflow.backend.dto.UserSummaryDto;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Endpoint for the currently authenticated user to update their own profile (e.g., name).
     *
     * @param currentUser The currently authenticated user (injected).
     * @param request     DTO containing the updated profile information.
     * @return ResponseEntity with the updated UserSummaryDto and status 200 (OK).
     */
    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can update their profile
    public ResponseEntity<UserSummaryDto> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("User {} updating their profile", currentUser.getEmail());
        UserSummaryDto updatedUser = userService.updateCurrentUserProfile(currentUser, request);
        return ResponseEntity.ok(updatedUser);
    }

    // Add endpoint for changing password separately
    // Add endpoint for getting current user details (/me) if needed
}