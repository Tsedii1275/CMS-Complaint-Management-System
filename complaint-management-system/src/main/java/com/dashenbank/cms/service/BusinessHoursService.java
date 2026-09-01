package com.dashenbank.cms.service;

import com.dashenbank.cms.model.HolidayCalendar;
import com.dashenbank.cms.repository.HolidayCalendarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BusinessHoursService {

    private final HolidayCalendarRepository holidayCalendarRepository;

    @Autowired
    public BusinessHoursService(HolidayCalendarRepository holidayCalendarRepository) {
        this.holidayCalendarRepository = holidayCalendarRepository;
    }

    // Working windows:
    // Mon-Thu: 08:00-12:00, 13:00-17:00
    // Fri:     08:00-11:30, 13:00-17:00
    // Sat:     08:00-12:00
    // Sun:     Closed

    private static final LocalTime MORNING_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_END_MON_THU = LocalTime.of(12, 0);
    private static final LocalTime MORNING_END_FRI = LocalTime.of(11, 30);
    private static final LocalTime MORNING_END_SAT = LocalTime.of(12, 0);

    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);

    public List<Map<String, Object>> getOfficialOperatingSchedule() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(scheduleRow("Monday – Thursday",
                formatWindow(MORNING_START, MORNING_END_MON_THU) + " & " + formatWindow(AFTERNOON_START, AFTERNOON_END),
                8.0, false));
        rows.add(scheduleRow("Friday",
                formatWindow(MORNING_START, MORNING_END_FRI) + " & " + formatWindow(AFTERNOON_START, AFTERNOON_END),
                7.5, false));
        rows.add(scheduleRow("Saturday",
                formatWindow(MORNING_START, MORNING_END_SAT),
                4.0, false));
        rows.add(scheduleRow("Sundays & Bank Holidays",
                "Closed — Java SLA timers pause (holiday_calendar)",
                0.0, true));
        return rows;
    }

    private Map<String, Object> scheduleRow(String days, String windows, double hoursPerDay, boolean closed) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("days", days);
        row.put("windows", windows);
        row.put("hoursPerDay", hoursPerDay);
        row.put("closed", closed);
        return row;
    }

    private String formatWindow(LocalTime start, LocalTime end) {
        return CLOCK.format(start) + " – " + CLOCK.format(end);
    }

    private Set<LocalDate> holidayCache = null;
    private long lastCacheTime = 0;

    private Set<LocalDate> getHolidayCache() {
        long now = System.currentTimeMillis();
        if (holidayCache == null || (now - lastCacheTime) > 60000) { // refresh every 60s
            try {
                holidayCache = new HashSet<>(holidayCalendarRepository.findAll()
                        .stream().map(HolidayCalendar::getHolidayDate).toList());
                lastCacheTime = now;
            } catch (Exception e) {
                return Collections.emptySet();
            }
        }
        return holidayCache;
    }

    public boolean isHoliday(LocalDate date) {
        return getHolidayCache().contains(date);
    }

    public boolean isWorkingDay(LocalDate date) {
        if (DayOfWeek.SUNDAY.equals(date.getDayOfWeek())) {
            return false;
        }
        return !isHoliday(date);
    }

    private LocalTime getMorningEndTime(DayOfWeek dow) {
        if (DayOfWeek.SATURDAY.equals(dow)) {
            return MORNING_END_SAT;
        }
        if (DayOfWeek.FRIDAY.equals(dow)) {
            return MORNING_END_FRI;
        }
        return MORNING_END_MON_THU;
    }

    public boolean isBusinessTime(LocalDateTime dt) {
        LocalDate date = dt.toLocalDate();
        if (!isWorkingDay(date)) {
            return false;
        }

        LocalTime time = dt.toLocalTime();
        DayOfWeek dow = date.getDayOfWeek();

        if (DayOfWeek.SATURDAY.equals(dow)) {
            return !time.isBefore(MORNING_START) && !time.isAfter(MORNING_END_SAT);
        }

        LocalTime morningEnd = getMorningEndTime(dow);

        boolean inMorning = !time.isBefore(MORNING_START) && !time.isAfter(morningEnd);
        boolean inAfternoon = !time.isBefore(AFTERNOON_START) && !time.isAfter(AFTERNOON_END);

        return inMorning || inAfternoon;
    }

    /**
     * Adds the specified number of business minutes to start, stepping forward only through business hours.
     */
    public LocalDateTime addBusinessMinutes(LocalDateTime start, int businessMinutesToAdd) {
        if (businessMinutesToAdd <= 0) {
            return start;
        }

        LocalDateTime current = snapToNextBusinessWindow(start);
        int remaining = businessMinutesToAdd;

        while (remaining > 0) {
            LocalDateTime windowEnd = getEndOfCurrentBusinessWindow(current);
            long minutesInWindow = Duration.between(current, windowEnd).toMinutes();

            if (remaining <= minutesInWindow) {
                return current.plusMinutes(remaining);
            } else {
                remaining -= minutesInWindow;
                current = snapToNextBusinessWindow(windowEnd.plusSeconds(1));
            }
        }
        return current;
    }

    /**
     * Calculates total business minutes elapsed between start and end.
     */
    public long calculateElapsedBusinessMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }

        LocalDateTime current = snapToNextBusinessWindow(start);
        long totalMinutes = 0;

        while (current.isBefore(end)) {
            LocalDateTime windowEnd = getEndOfCurrentBusinessWindow(current);
            if (end.isBefore(windowEnd)) {
                totalMinutes += Duration.between(current, end).toMinutes();
                break;
            } else {
                totalMinutes += Duration.between(current, windowEnd).toMinutes();
                current = snapToNextBusinessWindow(windowEnd.plusSeconds(1));
            }
        }
        return totalMinutes;
    }

    /**
     * Snaps a timestamp to the start of the current or next valid business window.
     */
    public LocalDateTime snapToNextBusinessWindow(LocalDateTime dt) {
        LocalDateTime curr = dt;
        while (true) {
            LocalDate date = curr.toLocalDate();
            if (!isWorkingDay(date)) {
                curr = LocalDateTime.of(date.plusDays(1), MORNING_START);
                continue;
            }

            LocalTime time = curr.toLocalTime();
            DayOfWeek dow = date.getDayOfWeek();

            if (DayOfWeek.SATURDAY.equals(dow)) {
                if (time.isBefore(MORNING_START)) {
                    return LocalDateTime.of(date, MORNING_START);
                } else if (time.isAfter(MORNING_END_SAT)) {
                    curr = LocalDateTime.of(date.plusDays(1), MORNING_START);
                    continue;
                } else {
                    return curr;
                }
            }

            LocalTime morningEnd = getMorningEndTime(dow);

            if (time.isBefore(MORNING_START)) {
                return LocalDateTime.of(date, MORNING_START);
            } else if (!time.isAfter(morningEnd)) {
                return curr;
            } else if (time.isBefore(AFTERNOON_START)) {
                return LocalDateTime.of(date, AFTERNOON_START);
            } else if (!time.isAfter(AFTERNOON_END)) {
                return curr;
            } else {
                curr = LocalDateTime.of(date.plusDays(1), MORNING_START);
            }
        }
    }

    private LocalDateTime getEndOfCurrentBusinessWindow(LocalDateTime dt) {
        LocalDate date = dt.toLocalDate();
        LocalTime time = dt.toLocalTime();
        DayOfWeek dow = date.getDayOfWeek();

        LocalTime morningEnd = getMorningEndTime(dow);

        if (DayOfWeek.SATURDAY.equals(dow)) {
            return LocalDateTime.of(date, MORNING_END_SAT);
        }

        if (!time.isAfter(morningEnd)) {
            return LocalDateTime.of(date, morningEnd);
        } else {
            return LocalDateTime.of(date, AFTERNOON_END);
        }
    }
}

