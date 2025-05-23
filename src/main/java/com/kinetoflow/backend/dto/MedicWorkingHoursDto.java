package com.kinetoflow.backend.dto;

import com.kinetoflow.backend.entity.MedicWorkingHours;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Builder
public record MedicWorkingHoursDto(
        Long id,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
    public static MedicWorkingHoursDto fromEntity(MedicWorkingHours entity) {
        if (entity == null) return null;
        return MedicWorkingHoursDto.builder()
                .id(entity.getId())
                .dayOfWeek(entity.getDayOfWeek())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
    }
} 