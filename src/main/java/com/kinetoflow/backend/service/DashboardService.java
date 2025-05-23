package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.DashboardStatsDto;
import com.kinetoflow.backend.entity.Company;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.enums.UserRole;
import com.kinetoflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getCompanyDashboardStats(User companyAdmin) {
        if (companyAdmin == null || companyAdmin.getCompany() == null) {
            log.error("Cannot fetch stats: Company Admin {} is not associated with a company.", companyAdmin != null ? companyAdmin.getEmail() : "null");
            // Or throw an exception depending on how this is called
            return DashboardStatsDto.builder().build(); // Return empty stats
        }

        Company company = companyAdmin.getCompany();
        log.info("Fetching dashboard stats for company ID: {}", company.getId());

        long activeMedicCount = userRepository.countByCompanyAndRoleAndIsActive(company, UserRole.MEDIC, true);
        long pendingMedicCount = userRepository.countByCompanyAndRoleAndIsActive(company, UserRole.MEDIC, false);
        long activePatientCount = userRepository.countByCompanyAndRoleAndIsActive(company, UserRole.USER, true);
        long pendingPatientCount = userRepository.countByCompanyAndRoleAndIsActive(company, UserRole.USER, false);
        long unassignedPatientCount = userRepository.countByCompanyAndRoleAndIsActiveAndAssignedMedicIsNull(company, UserRole.USER, true);


        return DashboardStatsDto.builder()
                .activeMedicCount(activeMedicCount)
                .pendingMedicCount(pendingMedicCount)
                .activePatientCount(activePatientCount)
                .pendingPatientCount(pendingPatientCount)
                .unassignedPatientCount(unassignedPatientCount)
                .build();
    }
}