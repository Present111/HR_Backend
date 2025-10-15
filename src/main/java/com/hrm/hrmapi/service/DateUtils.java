// src/main/java/com/hrm/hrmapi/service/DateUtils.java
package com.hrm.hrmapi.service;

import com.hrm.hrmapi.domain.Holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DateUtils {
    public static int countWorkingDays(LocalDate start, LocalDate end, List<Holiday> holidays) {
        Set<LocalDate> hs = new HashSet<>();
        for (var h : holidays) hs.add(h.getDate());

        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY) continue;
            if (hs.contains(d)) continue;
            count++;
        }
        return count;
    }

    public static boolean isWeekend(LocalDate d) {
        var w = d.getDayOfWeek();
        return w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY;
    }
}
