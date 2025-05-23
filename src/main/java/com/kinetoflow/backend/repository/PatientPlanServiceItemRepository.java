package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.PatientPlanServiceItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Might not need many custom queries here initially, depends on usage patterns
@Repository
public interface PatientPlanServiceItemRepository extends JpaRepository<PatientPlanServiceItem, Long> {

    // Fetch item with associated plan -> patient -> assignedMedic for authorization checks
    @EntityGraph(attributePaths = {"patientPlan", "patientPlan.patient", "patientPlan.patient.assignedMedic"})
    Optional<PatientPlanServiceItem> findDetailsById(Long id);
}