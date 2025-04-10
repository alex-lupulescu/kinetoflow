package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.PatientPlan;
import com.kinetoflow.backend.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientPlanRepository extends JpaRepository<PatientPlan, Long> {

    // Find active plans for a specific patient
    @EntityGraph(attributePaths = {"serviceItems", "serviceItems.service", "originatingPackage"})
    List<PatientPlan> findByPatientAndIsActiveTrue(User patient);

    // Find a specific plan with details, ensuring it belongs to the patient
    @EntityGraph(attributePaths = {"serviceItems", "serviceItems.service", "originatingPackage", "patient", "assignedBy"})
    Optional<PatientPlan> findByIdAndPatient(Long id, User patient);

    // Find ALL plans (active/inactive) for a specific patient, fetch details
    @EntityGraph(attributePaths = {"serviceItems", "serviceItems.service", "originatingPackage", "patient", "assignedBy"})
    List<PatientPlan> findByPatientOrderByIdDesc(User patient); // Order might be useful

    // Find a specific plan by ID, fetch details (used in service for authorization check)
    @EntityGraph(attributePaths = {"patient", "patient.assignedMedic"}) // Need patient and their medic
    Optional<PatientPlan> findById(Long id); // Override default findById to specify graph if needed often

    // Find NON-ARCHIVED plans for a specific patient (for primary display)
    @EntityGraph(attributePaths = {"serviceItems", "serviceItems.service", "originatingPackage", "patient", "assignedBy"})
    List<PatientPlan> findByPatientAndIsArchivedFalseOrderByIdDesc(User patient);

}