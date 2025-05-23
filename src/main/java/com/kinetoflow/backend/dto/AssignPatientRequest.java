package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignPatientRequest {
    // Can be null to unassign
    private Long medicId;
}