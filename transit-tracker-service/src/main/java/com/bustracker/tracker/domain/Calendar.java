package com.bustracker.tracker.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain model representing a calendar entry from GTFS calendar.txt
 * Defines regular weekly service patterns for transit services
 */
public class Calendar {

    private final String serviceId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean monday;
    private final boolean tuesday;
    private final boolean wednesday;
    private final boolean thursday;
    private final boolean friday;
    private final boolean saturday;
    private final boolean sunday;

    public Calendar(String serviceId, LocalDate startDate, LocalDate endDate,
                   boolean monday, boolean tuesday, boolean wednesday, boolean thursday,
                   boolean friday, boolean saturday, boolean sunday) {
        this.serviceId = Objects.requireNonNull(serviceId, "Service ID cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        this.endDate = Objects.requireNonNull(endDate, "End date cannot be null");
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    /**
     * Check if this service is active on the given date
     * @param date Date to check
     * @return true if service runs on this date according to the regular schedule
     */
    public boolean isActiveOnDate(LocalDate date) {
        // Check if date is within the service period
        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            return false;
        }

        // Check if service runs on this day of the week
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> monday;
            case TUESDAY -> tuesday;
            case WEDNESDAY -> wednesday;
            case THURSDAY -> thursday;
            case FRIDAY -> friday;
            case SATURDAY -> saturday;
            case SUNDAY -> sunday;
        };
    }

    /**
     * Parse GTFS date string (YYYYMMDD) to LocalDate
     */
    public static LocalDate parseGtfsDate(String dateStr) {
        if (dateStr == null || dateStr.length() != 8) {
            throw new IllegalArgumentException("Invalid GTFS date format: " + dateStr);
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.parse(dateStr, formatter);
    }

    /**
     * Parse GTFS boolean field (0 or 1)
     */
    public static boolean parseGtfsBoolean(String boolStr) {
        if ("1".equals(boolStr)) {
            return true;
        } else if ("0".equals(boolStr)) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid GTFS boolean value: " + boolStr);
        }
    }

    // Getters
    public String getServiceId() { return serviceId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isMonday() { return monday; }
    public boolean isTuesday() { return tuesday; }
    public boolean isWednesday() { return wednesday; }
    public boolean isThursday() { return thursday; }
    public boolean isFriday() { return friday; }
    public boolean isSaturday() { return saturday; }
    public boolean isSunday() { return sunday; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calendar calendar = (Calendar) o;
        return Objects.equals(serviceId, calendar.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }

    @Override
    public String toString() {
        return String.format("Calendar{serviceId='%s', period=%s to %s, days=%s%s%s%s%s%s%s}",
            serviceId, startDate, endDate,
            monday ? "M" : "-",
            tuesday ? "T" : "-",
            wednesday ? "W" : "-",
            thursday ? "T" : "-",
            friday ? "F" : "-",
            saturday ? "S" : "-",
            sunday ? "S" : "-"
        );
    }
}