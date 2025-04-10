package com.kinetoflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 100)
    private String name;

    // Password update would typically be a separate endpoint/DTO for security
    // Include email update only if allowed (requires verification usually)
}