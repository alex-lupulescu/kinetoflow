package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePlanStatusRequest {
    @NotNull(message = "Plan active status cannot be null")
    private Boolean isActive;
}