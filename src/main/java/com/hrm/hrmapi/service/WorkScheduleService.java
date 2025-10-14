package com.hrm.hrmapi.service;

import com.hrm.hrmapi.domain.WorkSchedule;
import com.hrm.hrmapi.repo.WorkScheduleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Instant;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class WorkScheduleService {
    private final WorkScheduleRepo repo;

    /**
     * Lấy schedule hiện tại, hoặc tạo default nếu chưa có
     */
    public WorkSchedule getOrDefault() {
        return repo.findAll().stream()
                .findFirst()
                .orElseGet(this::createDefault);
    }

    /**
     * Upsert schedule (tạo mới hoặc update existing)
     */
    public WorkSchedule upsert(WorkSchedule s) {
        s.setUpdatedAt(Instant.now());

        // Đảm bảo có working days
        if (s.getWorkingDays() == null || s.getWorkingDays().isEmpty()) {
            s.setWorkingDays(EnumSet.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
            ));
        }

        // Đảm bảo các giá trị numeric không null
        if (s.getBreakMinutes() == null) s.setBreakMinutes(60);
        if (s.getGraceLateMinutes() == null) s.setGraceLateMinutes(5);
        if (s.getGraceEarlyMinutes() == null) s.setGraceEarlyMinutes(0);
        if (s.getOtAfterMinutes() == null) s.setOtAfterMinutes(30);
        if (s.getOtRoundToMinutes() == null) s.setOtRoundToMinutes(15);

        return repo.save(s);
    }

    /**
     * Tạo default schedule
     */
    private WorkSchedule createDefault() {
        var def = WorkSchedule.builder()
                .name("Default")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .graceLateMinutes(5)
                .graceEarlyMinutes(0)
                .otAfterMinutes(30)
                .otRoundToMinutes(15)
                .workingDays(EnumSet.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                ))
                .updatedAt(Instant.now())
                .build();
        return repo.save(def);
    }
}