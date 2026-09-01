package com.dashenbank.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "holiday_calendar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", length = 150, nullable = false)
    private String holidayName;

    @Column(name = "holiday_type", length = 50)
    private String holidayType; // PUBLIC_HOLIDAY, BANK_HOLIDAY
}
