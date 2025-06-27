package com.bustracker.tracker.domain;

import java.util.Objects;

/**
 * Domain model representing a bus stop from GTFS stops.txt
 * Immutable value object with geographic coordinates
 */
public class Stop {

  private final String stopId;
  private final String stopName;
  private final double stopLat;
  private final double stopLon;

  public Stop(String stopId, String stopName, double stopLat, double stopLon) {
    this.stopId = Objects.requireNonNull(stopId, "Stop ID cannot be null");
    this.stopName = Objects.requireNonNull(stopName, "Stop name cannot be null");
    this.stopLat = stopLat;
    this.stopLon = stopLon;

    // Basic validation for Vancouver area coordinates
    if (stopLat < 49.0 || stopLat > 49.5) {
      throw new IllegalArgumentException("Invalid latitude for Vancouver area: " + stopLat);
    }
    if (stopLon < -123.5 || stopLon > -122.0) {
      throw new IllegalArgumentException("Invalid longitude for Vancouver area: " + stopLon);
    }
  }

  public String getStopId() {
    return stopId;
  }

  public String getStopName() {
    return stopName;
  }

  public double getStopLat() {
    return stopLat;
  }

  public double getStopLon() {
    return stopLon;
  }

  /**
   * Calculate distance to another geographic point using Haversine formula
   */
  public double distanceToPoint(double lat, double lon) {
    final double R = 6371000; // Earth's radius in meters
    double dLat = Math.toRadians(lat - this.stopLat);
    double dLon = Math.toRadians(lon - this.stopLon);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(this.stopLat)) * Math.cos(Math.toRadians(lat)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Stop stop = (Stop) o;
    return Objects.equals(stopId, stop.stopId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stopId);
  }

  @Override
  public String toString() {
    return String.format("Stop{id='%s', name='%s', lat=%.4f, lon=%.4f}",
        stopId, stopName, stopLat, stopLon);
  }
}