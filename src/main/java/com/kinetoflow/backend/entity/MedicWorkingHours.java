package com.kinetoflow.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "medic_working_hours")
public class MedicWorkingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medic_id", nullable = false)
    private User medic; // User with ROLE_MEDIC

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek; // e.g., MONDAY, TUESDAY

    @NotNull
    @Column(nullable = false)
    private LocalTime startTime; // e.g., 09:00

    @NotNull
    @Column(nullable = false)
    private LocalTime endTime; // e.g., 17:00

    // Standard equals and hashCode based on id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicWorkingHours that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
} 