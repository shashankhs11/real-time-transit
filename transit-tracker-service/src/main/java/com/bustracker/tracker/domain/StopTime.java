package com.bustracker.tracker.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Domain model representing a stop time from GTFS stop_times.txt
 * Links trips to stops with timing and sequence information
 * This is the CORE model that connects everything together
 */
public class StopTime {

  private final String tripId;
  private final String stopId;
  private final LocalTime arrivalTime;
  private final LocalTime departureTime;
  private final int stopSequence;

  public StopTime(String tripId, String stopId, LocalTime arrivalTime,
      LocalTime departureTime, int stopSequence) {
    this.tripId = Objects.requireNonNull(tripId, "Trip ID cannot be null");
    this.stopId = Objects.requireNonNull(stopId, "Stop ID cannot be null");
    this.arrivalTime = Objects.requireNonNull(arrivalTime, "Arrival time cannot be null");
    this.departureTime = departureTime != null ? departureTime : arrivalTime; // Default to arrival time
    this.stopSequence = stopSequence;

    if (stopSequence < 0) {
      throw new IllegalArgumentException("Stop sequence must be non-negative: " + stopSequence);
    }
  }

  public String getTripId() {
    return tripId;
  }

  public String getStopId() {
    return stopId;
  }

  public LocalTime getArrivalTime() {
    return arrivalTime;
  }

  public LocalTime getDepartureTime() {
    return departureTime;
  }

  public int getStopSequence() {
    return stopSequence;
  }

  /**
   * Parse GTFS time format (HH:mm:ss) which can exceed 24 hours
   * e.g., "25:30:00" means 1:30 AM next day
   */
  public static LocalTime parseGtfsTime(String timeStr) {
    if (timeStr == null || timeStr.trim().isEmpty()) {
      return null;
    }

    String[] parts = timeStr.split(":");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid time format: " + timeStr);
    }

    int hours = Integer.parseInt(parts[0]);
    int minutes = Integer.parseInt(parts[1]);
    int seconds = Integer.parseInt(parts[2]);

    // Handle times beyond 24 hours (GTFS allows this)
    hours = hours % 24;

    return LocalTime.of(hours, minutes, seconds);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StopTime stopTime = (StopTime) o;
    return Objects.equals(tripId, stopTime.tripId) &&
        Objects.equals(stopId, stopTime.stopId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tripId, stopId);
  }

  @Override
  public String toString() {
    return String.format("StopTime{tripId='%s', stopId='%s', seq=%d, arrival=%s}",
        tripId, stopId, stopSequence, arrivalTime);
  }
}