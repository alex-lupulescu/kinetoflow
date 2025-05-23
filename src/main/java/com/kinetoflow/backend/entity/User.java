package com.kinetoflow.backend.entity;

import com.kinetoflow.backend.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
// Removed import for List as it's not directly used here anymore for users field

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(max = 120) // Max length for hashed password
    @Column(nullable = false)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean isActive = false; // Accounts start inactive until invitation is accepted

    @Column(length = 64)
    private String invitationToken;

    private LocalDateTime invitationTokenExpiryDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Many users belong to one company (except APP_ADMINs who might have null company)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id") // Foreign key column in the 'users' table
    private Company company;

    // --- New Relationship: Assigned Medic (for Patients) ---
    // A Patient (USER role) can be assigned to one Medic (MEDIC role)
    // This field is relevant primarily for users with the USER role.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_medic_id", referencedColumnName = "id") // FK to the User table itself
    private User assignedMedic; // Null if not a patient or not assigned


    // --- UserDetails Implementation (for Spring Security) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // Optional: toString method (excluding sensitive/lazy fields)
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                ", companyId=" + (company != null ? company.getId() : null) +
                ", assignedMedicId=" + (assignedMedic != null ? assignedMedic.getId() : null) +
                '}';
    }
}