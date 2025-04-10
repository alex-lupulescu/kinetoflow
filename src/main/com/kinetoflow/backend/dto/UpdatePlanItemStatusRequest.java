package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePlanItemStatusRequest {
    @NotNull(message = "Item active status cannot be null")
    private Boolean isItemActive;
}