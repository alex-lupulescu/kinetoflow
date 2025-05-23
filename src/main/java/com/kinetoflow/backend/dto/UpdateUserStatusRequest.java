package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data; // Use @Data for simple mutable DTOs if needed, or record/builder

@Data // Includes getters, setters, toString, equals, hashCode
public class UpdateUserStatusRequest {
    @NotNull(message = "Active status cannot be null")
    private Boolean isActive;
}