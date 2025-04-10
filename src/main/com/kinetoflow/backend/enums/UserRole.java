
package com.kinetoflow.backend.enums;

public enum UserRole {
    APP_ADMIN,      // Super admin for the entire application
    COMPANY_ADMIN,  // Admin for a specific therapy company
    MEDIC,          // Therapist/Medic within a company
    USER            // Patient/Client linked to a company and potentially a medic
}
