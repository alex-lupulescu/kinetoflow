package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.Appointment;
import com.kinetoflow.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Find appointments for a specific medic within a date range
    @Query("SELECT a FROM Appointment a WHERE a.medic = :medic AND " +
            "((a.scheduledStartTime >= :startDate AND a.scheduledStartTime < :endDate) OR " +
            "(a.scheduledEndTime > :startDate AND a.scheduledEndTime <= :endDate) OR " +
            "(a.scheduledStartTime < :startDate AND a.scheduledEndTime > :endDate))")
    List<Appointment> findByMedicAndDateTimeRange(
            @Param("medic") User medic,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find appointments for a specific patient within a date range
    @Query("SELECT a FROM Appointment a WHERE a.patient = :patient AND " +
            "((a.scheduledStartTime >= :startDate AND a.scheduledStartTime < :endDate) OR " +
            "(a.scheduledEndTime > :startDate AND a.scheduledEndTime <= :endDate) OR " +
            "(a.scheduledStartTime < :startDate AND a.scheduledEndTime > :endDate))")
    List<Appointment> findByPatientAndDateTimeRange(
            @Param("patient") User patient,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Add more specific queries as needed (e.g., find by status, find upcoming)
}