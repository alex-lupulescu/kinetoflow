package com.kinetoflow.backend.controller;

import com.kinetoflow.backend.dto.DashboardStatsDto;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/company-stats")
    @PreAuthorize("hasRole('COMPANY_ADMIN')") // Only company admins can get these stats
    public ResponseEntity<DashboardStatsDto> getCompanyStats(@AuthenticationPrincipal User currentUser) {
        // @AuthenticationPrincipal injects the authenticated User object
        log.info("Request received for company dashboard stats by user: {}", currentUser.getEmail());
        DashboardStatsDto stats = dashboardService.getCompanyDashboardStats(currentUser);
        return ResponseEntity.ok(stats);
    }

    // Add endpoints for APP_ADMIN dashboard stats later if needed
}