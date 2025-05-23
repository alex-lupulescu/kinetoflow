package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateCompanyRequest(
        // Name might not be updatable, depends on requirements
        // @NotBlank(message = "Company name cannot be blank")
        // @Size(max = 100, message = "Company name cannot exceed 100 characters")
        // String name,

        @Size(max = 255, message = "Address cannot exceed 255 characters")
        String address // Allow updating address
) {}