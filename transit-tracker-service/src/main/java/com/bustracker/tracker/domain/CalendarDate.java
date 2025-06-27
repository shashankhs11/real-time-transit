package com.bustracker.tracker.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain model representing a calendar date exception from GTFS calendar_dates.txt
 * Defines exceptions to regular service patterns (additions or removals)
 */
public class CalendarDate {

    private final String serviceId;
    private final LocalDate date;
    private final ExceptionType exceptionType;

    public CalendarDate(String serviceId, LocalDate date, ExceptionType exceptionType) {
        this.serviceId = Objects.requireNonNull(serviceId, "Service ID cannot be null");
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        this.exceptionType = Objects.requireNonNull(exceptionType, "Exception type cannot be null");
    }

    public CalendarDate(String serviceId, LocalDate date, int exceptionTypeValue) {
        this(serviceId, date, ExceptionType.fromValue(exceptionTypeValue));
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
     * Check if this exception adds service for the date
     */
    public boolean isServiceAdded() {
        return exceptionType == ExceptionType.SERVICE_ADDED;
    }

    /**
     * Check if this exception removes service for the date
     */
    public boolean isServiceRemoved() {
        return exceptionType == ExceptionType.SERVICE_REMOVED;
    }

    // Getters
    public String getServiceId() { return serviceId; }
    public LocalDate getDate() { return date; }
    public ExceptionType getExceptionType() { return exceptionType; }
    public int getExceptionTypeValue() { return exceptionType.getValue(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarDate that = (CalendarDate) o;
        return Objects.equals(serviceId, that.serviceId) &&
               Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, date);
    }

    @Override
    public String toString() {
        return String.format("CalendarDate{serviceId='%s', date=%s, type=%s}",
            serviceId, date, exceptionType);
    }

    /**
     * GTFS exception types for calendar_dates.txt
     */
    public enum ExceptionType {
        SERVICE_ADDED(1, "Service Added"),
        SERVICE_REMOVED(2, "Service Removed");

        private final int value;
        private final String description;

        ExceptionType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Get ExceptionType from GTFS integer value
         */
        public static ExceptionType fromValue(int value) {
            return switch (value) {
                case 1 -> SERVICE_ADDED;
                case 2 -> SERVICE_REMOVED;
                default -> throw new IllegalArgumentException("Invalid exception type value: " + value);
            };
        }

        /**
         * Parse ExceptionType from string value
         */
        public static ExceptionType fromString(String valueStr) {
            try {
                int value = Integer.parseInt(valueStr.trim());
                return fromValue(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid exception type string: " + valueStr, e);
            }
        }

        @Override
        public String toString() {
            return String.format("%s(%d)", description, value);
        }
    }
}