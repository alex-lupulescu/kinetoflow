package com.kinetoflow.backend.entity;

import com.kinetoflow.backend.enums.AppointmentStatus; // Create this enum
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient; // User with ROLE_USER

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medic_id", nullable = false)
    private User medic; // User with ROLE_MEDIC

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service; // The specific service being performed

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // Denormalized for easier querying? Or rely on patient/medic company

    @NotNull(message = "Scheduled start time is required")
    @Column(nullable = false)
    // @Future(message = "Start time must be in the future") // Validation might be better in service layer
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "Scheduled end time is required")
    @Column(nullable = false)
    // @Future(message = "End time must be in the future")
    private LocalDateTime scheduledEndTime;

    private LocalDateTime actualStartTime; // Optional: Track actual start
    private LocalDateTime actualEndTime;   // Optional: Track actual end

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED; // Default status

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes; // Medic notes for the appointment

    // Link to the specific plan item being consumed (Null if not part of a plan/package)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_plan_service_item_id")
    private PatientPlanServiceItem patientPlanServiceItem; // Requires PatientPlanServiceItem entity (Phase 8)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean sessionConsumed = false;

    // --- Basic equals/hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}