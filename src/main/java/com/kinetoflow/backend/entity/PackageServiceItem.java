package com.kinetoflow.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "package_service_items", uniqueConstraints = {
        // Ensure a service appears only once per package
        @UniqueConstraint(columnNames = {"package_id", "service_id"})
})
public class PackageServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private Package pack; // Owning side of Package relationship

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service; // The service included

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    // equals/hashCode - Based on Package and Service for logical uniqueness within a set
    // Ensure IDs are used if available for persistent entities.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackageServiceItem that)) return false;

        // If both have IDs, compare by ID (most reliable for persisted entities)
        if (this.id != null && that.id != null) {
            return this.id.equals(that.id);
        }
        // If IDs aren't available (transient), compare by package and service references/IDs
        boolean packageEquals = (this.pack == null && that.pack == null) ||
                (this.pack != null && that.pack != null &&
                        (this.pack.getId() != null ? this.pack.getId().equals(that.pack.getId()) : this.pack == that.pack)); // Fallback to reference

        boolean serviceEquals = (this.service == null && that.service == null) ||
                (this.service != null && that.service != null &&
                        (this.service.getId() != null ? this.service.getId().equals(that.service.getId()) : this.service == that.service)); // Fallback to reference

        return packageEquals && serviceEquals;
    }

    @Override
    public int hashCode() {
        int result = pack != null && pack.getId() != null ? pack.getId().hashCode() : 0;
        result = 31 * result + (service != null && service.getId() != null ? service.getId().hashCode() : 0);
        // Use a constant if IDs are null to avoid hashcode changing if compared before/after save
        // Or rely on reference hashcode if both are transient.
        return result == 0 ? System.identityHashCode(this) : result;
    }

    @Override
    public String toString() {
        // Avoid deep nesting in toString
        return "PackageServiceItem{" +
                "id=" + id +
                ", packageId=" + (pack != null ? pack.getId() : null) +
                ", serviceId=" + (service != null ? service.getId() : null) +
                ", quantity=" + quantity +
                '}';
    }
}