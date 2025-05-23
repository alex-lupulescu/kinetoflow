package com.kinetoflow.backend.repository;

import com.kinetoflow.backend.entity.MedicWorkingHours;
import com.kinetoflow.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicWorkingHoursRepository extends JpaRepository<MedicWorkingHours, Long> {

    List<MedicWorkingHours> findByMedic(User medic);

    List<MedicWorkingHours> findByMedicId(Long medicId);

    Optional<MedicWorkingHours> findByMedicAndDayOfWeek(User medic, DayOfWeek dayOfWeek);

    void deleteByMedicAndDayOfWeek(User medic, DayOfWeek dayOfWeek);
} 