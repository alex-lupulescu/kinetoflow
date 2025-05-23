package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.TimeBlock;
import com.kinetoflow.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, Long> {

    // Find time blocks for a specific medic within a date range
    @Query("SELECT tb FROM TimeBlock tb WHERE tb.medic = :medic AND " +
            "((tb.startTime >= :startDate AND tb.startTime < :endDate) OR " +
            "(tb.endTime > :startDate AND tb.endTime <= :endDate) OR " +
            "(tb.startTime < :startDate AND tb.endTime > :endDate))")
    List<TimeBlock> findByMedicAndDateTimeRange(
            @Param("medic") User medic,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

}