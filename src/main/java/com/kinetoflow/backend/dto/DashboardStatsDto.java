package com.kinetoflow.backend.dto;

import lombok.Builder;

@Builder
public record DashboardStatsDto(
        long activeMedicCount,
        long pendingMedicCount,
        long activePatientCount,
        long pendingPatientCount,
        long unassignedPatientCount // Patients without an assigned medic
) {}