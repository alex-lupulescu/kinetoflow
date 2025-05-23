package com.kinetoflow.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "packages")
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Package name cannot be blank")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Min(value = 0, message = "Total price cannot be negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice; // Optional package price

    @NotNull(message = "Active status is required")
    @Column(nullable = false)
    private Boolean isActive = true;

    @NotNull // A package must belong to a company
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // --- Relationship to Services via PackageServiceItem ---
    // Owning side of the relationship
    // Cascade ALL: if package is deleted, its items are deleted.
    // orphanRemoval=true: if an item is removed from the Java Set, it's deleted from DB.
    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // 'pack' matches field name in PackageServiceItem
    @Builder.Default // Initialize with Lombok Builder
    @OrderBy("id") // Optional: Keep items ordered if needed
    private Set<PackageServiceItem> items = new HashSet<>();

    // Helper methods for managing service items (ensures bidirectional consistency)
    public void addServiceItem(PackageServiceItem item) {
        items.add(item);
        item.setPack(this); // Set the owning side
    }

    public void removeServiceItem(PackageServiceItem item) {
        items.remove(item);
        item.setPack(null); // Unset the owning side
    }

    // Clear all items - useful for updates
    public void clearItems() {
        // Iterate to ensure orphanRemoval is triggered correctly if needed by JPA provider
        // Or just rely on items.clear() if orphanRemoval=true handles it
        this.items.forEach(item -> item.setPack(null)); // Break association first
        this.items.clear();
    }

    // equals/hashCode based on ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Package aPackage)) return false; // Use instanceof pattern matching
        return id != null && id.equals(aPackage.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        // Avoid including lazy-loaded or collection fields
        return "Package{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", totalPrice=" + totalPrice +
                ", isActive=" + isActive +
                ", companyId=" + (company != null ? company.getId() : null) +
                '}';
    }
}