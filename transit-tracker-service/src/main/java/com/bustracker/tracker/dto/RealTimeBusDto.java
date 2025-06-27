package com.bustracker.tracker.dto;

import java.time.Instant;

/**
 * Data Transfer Object for real-time bus information
 * Represents a bus currently en route to the stop
 */
public class RealTimeBusDto {

  private String vehicleId;
  private String tripId;
  private int etaMinutes;
  private int etaSeconds;
  private double distanceMeters;
  private String currentStatus;
  private Instant lastUpdated;

  // Default constructor
  public RealTimeBusDto() {}

  public RealTimeBusDto(String vehicleId, String tripId, int etaMinutes, int etaSeconds,
      double distanceMeters, String currentStatus, Instant lastUpdated) {
    this.vehicleId = vehicleId;
    this.tripId = tripId;
    this.etaMinutes = etaMinutes;
    this.etaSeconds = etaSeconds;
    this.distanceMeters = distanceMeters;
    this.currentStatus = currentStatus;
    this.lastUpdated = lastUpdated;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getTripId() {
    return tripId;
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  public int getEtaMinutes() {
    return etaMinutes;
  }

  public void setEtaMinutes(int etaMinutes) {
    this.etaMinutes = etaMinutes;
  }

  public int getEtaSeconds() {
    return etaSeconds;
  }

  public void setEtaSeconds(int etaSeconds) {
    this.etaSeconds = etaSeconds;
  }

  public double getDistanceMeters() {
    return distanceMeters;
  }

  public void setDistanceMeters(double distanceMeters) {
    this.distanceMeters = distanceMeters;
  }

  public String getCurrentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(String currentStatus) {
    this.currentStatus = currentStatus;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @Override
  public String toString() {
    return String.format("RealTimeBusDto{vehicleId='%s', etaMinutes=%d, distanceMeters=%.0f}",
        vehicleId, etaMinutes, distanceMeters);
  }
}
