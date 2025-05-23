package com.kinetoflow.backend.service;

import com.kinetoflow.backend.dto.MedicWorkingHoursDto;
import com.kinetoflow.backend.entity.MedicWorkingHours;
import com.kinetoflow.backend.entity.User;
import com.kinetoflow.backend.exception.BadRequestException;
import com.kinetoflow.backend.repository.MedicWorkingHoursRepository;
import com.kinetoflow.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicWorkingHoursService {

    private final MedicWorkingHoursRepository workingHoursRepository;
    private final UserRepository userRepository; // Assuming you need to fetch User if not passed directly

    @Transactional
    public MedicWorkingHoursDto setWorkingHours(User medic, MedicWorkingHoursDto dto) {
        if (dto.dayOfWeek() == null || dto.startTime() == null || dto.endTime() == null) {
            throw new BadRequestException("Day of week, start time, and end time are required.");
        }
        if (dto.startTime().isAfter(dto.endTime()) || dto.startTime().equals(dto.endTime())) {
            throw new BadRequestException("Start time must be before end time.");
        }

        MedicWorkingHours workingHours = workingHoursRepository.findByMedicAndDayOfWeek(medic, dto.dayOfWeek())
                .orElseGet(() -> MedicWorkingHours.builder().medic(medic).dayOfWeek(dto.dayOfWeek()).build());

        workingHours.setStartTime(dto.startTime());
        workingHours.setEndTime(dto.endTime());

        MedicWorkingHours saved = workingHoursRepository.save(workingHours);
        log.info("Set working hours for medic ID {} on {} from {} to {}", medic.getId(), saved.getDayOfWeek(), saved.getStartTime(), saved.getEndTime());
        return MedicWorkingHoursDto.fromEntity(saved);
    }

    @Transactional
    public List<MedicWorkingHoursDto> setMultipleWorkingHours(User medic, List<MedicWorkingHoursDto> dtoList) {
        // Clear existing working hours for this medic before setting new ones
        log.info("Clearing existing working hours for medic ID {}", medic.getId());
        workingHoursRepository.deleteAll(workingHoursRepository.findByMedic(medic));
        
        log.info("Setting new working hours for medic ID {}: {}", medic.getId(), dtoList);
        return dtoList.stream()
                .map(dto -> setWorkingHours(medic, dto)) // This reuses the single day set/upsert logic
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicWorkingHoursDto> getWorkingHours(User medic) {
        return workingHoursRepository.findByMedic(medic).stream()
                .map(MedicWorkingHoursDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteWorkingHoursForDay(User medic, DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            throw new BadRequestException("Day of week is required to delete working hours.");
        }
        workingHoursRepository.deleteByMedicAndDayOfWeek(medic, dayOfWeek);
        log.info("Deleted working hours for medic ID {} on {}", medic.getId(), dayOfWeek);
    }
    
    @Transactional
    public void clearAllWorkingHours(User medic) {
        List<MedicWorkingHours> hoursToDelete = workingHoursRepository.findByMedic(medic);
        if (!hoursToDelete.isEmpty()) {
            workingHoursRepository.deleteAll(hoursToDelete);
            log.info("Cleared all working hours for medic ID {}", medic.getId());
        } else {
            log.info("No working hours found to clear for medic ID {}", medic.getId());
        }
    }
} 