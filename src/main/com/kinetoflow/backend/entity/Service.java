package com.kinetoflow.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*; // Using individual annotations for clarity
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet; // Import HashSet
import java.util.Set; // Import Set

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Add builder
@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Service name cannot be blank")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Lob // Use Lob for potentially longer descriptions. Recommended over columnDefinition="TEXT" for portability.
    @Column(columnDefinition = "TEXT") // Still good practice for some DBs
    private String description;

    @NotNull(message = "Duration cannot be null")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(nullable = false)
    private Integer durationMinutes; // Duration in minutes

    @Min(value = 0, message = "Price cannot be negative") // Add validation
    @Column(precision = 10, scale = 2) // Suitable for currency
    private BigDecimal price; // Optional price

    @Size(max = 50)
    private String category; // Optional category

    @NotNull(message = "Active status is required")
    @Column(nullable = false)
    private Boolean isActive = true; // Default to active

    @NotNull // A service must belong to a company
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // --- Relationship to Package Items ---
    // One service can be part of many PackageServiceItems
    // Inverse side: Not strictly needed unless navigating from Service -> Packages
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY) // Cascade usually not needed on inverse side
    @Builder.Default // Initialize with Lombok Builder if used
    private Set<PackageServiceItem> packageItems = new HashSet<>();

    // equals/hashCode based on ID for proper Set behavior and JPA identity
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use instanceof check for proxy compatibility
        if (!(o instanceof Service service)) return false;
        // Only compare IDs if both are non-null (persistent entities)
        return id != null && id.equals(service.getId());
    }

    @Override
    public int hashCode() {
        // Use a constant for transient entities, or ID hashcode for persistent ones
        return id != null ? id.hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        // Avoid including lazy-loaded or collection fields in default toString
        return "Service{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", price=" + price +
                ", isActive=" + isActive +
                ", companyId=" + (company != null ? company.getId() : null) + // Avoid loading company proxy
                '}';
    }
}