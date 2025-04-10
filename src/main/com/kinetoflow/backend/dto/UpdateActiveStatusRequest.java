package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data; // Use @Data or record/builder

@Data // Using @Data for simplicity for this single-field mutable request
public class UpdateActiveStatusRequest {
    @NotNull(message = "Active status cannot be null")
    private Boolean isActive;
}