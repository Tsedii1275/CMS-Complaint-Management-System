package com.dashenbank.cms.repository;

import com.dashenbank.cms.model.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Long> {
    Optional<HolidayCalendar> findByHolidayDate(LocalDate holidayDate);
    boolean existsByHolidayDate(LocalDate holidayDate);
}
