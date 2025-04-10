package com.kinetoflow.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Where; // Import Where

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patient_plan_service_items")
@Check(constraints = "remaining_quantity <= total_quantity")
// Optional: Default filter
// @Where(clause = "is_archived = false")
public class PatientPlanServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_plan_id", nullable = false)
    private PatientPlan patientPlan;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @NotNull(message = "Total quantity must be specified")
    @Min(value = 1, message = "Total quantity must be at least 1")
    @Column(nullable = false)
    private Integer totalQuantity;

    @NotNull(message = "Remaining quantity must be specified")
    @Min(value = 0, message = "Remaining quantity cannot be negative")
    @Column(nullable = false)
    private Integer remainingQuantity;

    @Min(value = 0, message = "Price cannot be negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    @NotNull(message = "Item active status must be specified")
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isItemActive = true;

    // --- NEW FIELD for Archive ---
    @NotNull(message = "Archived status must be specified") // Added validation
    @Column(name="is_item_archived", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE") // Explicit column name + default
    private Boolean isArchived = false;
    // --- END NEW FIELD ---

    public boolean decrementRemainingQuantity() {
        // Only allow decrement if BOTH plan and item are active and not archived
        if (Boolean.TRUE.equals(this.isItemActive) && Boolean.FALSE.equals(this.isArchived) &&
                this.patientPlan != null && Boolean.TRUE.equals(this.patientPlan.getIsActive()) && Boolean.FALSE.equals(this.patientPlan.getIsArchived()) &&
                this.remainingQuantity > 0) {
            this.remainingQuantity--;
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientPlanServiceItem that)) return false;
        if (this.id != null && that.id != null) return this.id.equals(that.id); // Use ID if available
        // Fallback for transient entities (less reliable for Sets before save)
        boolean planEquals = (this.patientPlan == null && that.patientPlan == null) ||
                (this.patientPlan != null && this.patientPlan == that.patientPlan); // Reference equality
        boolean serviceEquals = (this.service == null && that.service == null) ||
                (this.service != null && this.service == that.service); // Reference equality
        return planEquals && serviceEquals;
    }

    @Override
    public int hashCode() {
        // Simple hashcode for transient entities, use ID for persistent
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "PatientPlanServiceItem{" +
                "id=" + id +
                ", patientPlanId=" + (patientPlan != null ? patientPlan.getId() : null) +
                ", serviceId=" + (service != null ? service.getId() : null) +
                ", totalQuantity=" + totalQuantity +
                ", remainingQuantity=" + remainingQuantity +
                ", isItemActive=" + isItemActive +
                ", isArchived=" + isArchived + // Added
                '}';
    }
}