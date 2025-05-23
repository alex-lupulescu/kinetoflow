package com.kinetoflow.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patient_plans")
// Optional: Default filter for repositories if you *always* want to exclude archived
// @Where(clause = "is_archived = false")
public class PatientPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient; // The patient this plan belongs to (Role=USER)

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy; // Who created/assigned this plan (Medic or Admin)

    @NotNull // Must belong to the same company as patient/assigner
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Optional link back to the Package template if this plan was created from one
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "originating_package_id")
    private Package originatingPackage;

    @Column(nullable = false)
    private Boolean isActive = true; // Is the overall plan currently active?

    // --- NEW FIELD for Archive ---
    @NotNull(message = "Archived status must be specified") // Added validation
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isArchived = false;
    // --- END NEW FIELD ---

    private LocalDateTime assignedDate; // When the plan was assigned/created

    private LocalDateTime expiryDate; // Optional: When the plan/package expires

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes; // General notes about the plan

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // --- Relationship to Service Items ---
    @OneToMany(mappedBy = "patientPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("id")
    // Optional: Filter archived items at the association level
    // @Where(clause = "is_item_archived = false")
    private Set<PatientPlanServiceItem> serviceItems = new HashSet<>();

    // Helper methods
    public void addServiceItem(PatientPlanServiceItem item) {
        serviceItems.add(item);
        item.setPatientPlan(this);
    }

    public void removeServiceItem(PatientPlanServiceItem item) {
        serviceItems.remove(item);
        item.setPatientPlan(null);
    }

    public void clearItems() {
        this.serviceItems.forEach(item -> item.setPatientPlan(null));
        this.serviceItems.clear();
    }

    // equals/hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientPlan that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PatientPlan{" +
                "id=" + id +
                ", patientId=" + (patient != null ? patient.getId() : null) +
                ", isActive=" + isActive +
                ", isArchived=" + isArchived + // Added
                ", companyId=" + (company != null ? company.getId() : null) +
                '}';
    }
}