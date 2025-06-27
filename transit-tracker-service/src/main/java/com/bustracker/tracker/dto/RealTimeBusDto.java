package com.bustracker.tracker.dto;

import java.time.Instant;
import java.time.LocalTime;

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
  
  // Scheduled time information
  private LocalTime scheduledArrival;
  private Integer delayMinutes;
  private String delayStatus;

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
    // Scheduled fields will be null by default
  }

  public RealTimeBusDto(String vehicleId, String tripId, int etaMinutes, int etaSeconds,
      double distanceMeters, String currentStatus, Instant lastUpdated,
      LocalTime scheduledArrival, Integer delayMinutes, String delayStatus) {
    this.vehicleId = vehicleId;
    this.tripId = tripId;
    this.etaMinutes = etaMinutes;
    this.etaSeconds = etaSeconds;
    this.distanceMeters = distanceMeters;
    this.currentStatus = currentStatus;
    this.lastUpdated = lastUpdated;
    this.scheduledArrival = scheduledArrival;
    this.delayMinutes = delayMinutes;
    this.delayStatus = delayStatus;
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

  public LocalTime getScheduledArrival() {
    return scheduledArrival;
  }

  public void setScheduledArrival(LocalTime scheduledArrival) {
    this.scheduledArrival = scheduledArrival;
  }

  public Integer getDelayMinutes() {
    return delayMinutes;
  }

  public void setDelayMinutes(Integer delayMinutes) {
    this.delayMinutes = delayMinutes;
  }

  public String getDelayStatus() {
    return delayStatus;
  }

  public void setDelayStatus(String delayStatus) {
    this.delayStatus = delayStatus;
  }

  /**
   * Helper method to check if this bus has scheduled time information
   */
  public boolean hasScheduledTime() {
    return scheduledArrival != null;
  }

  /**
   * Helper method to check if this bus is delayed
   */
  public boolean isDelayed() {
    return delayMinutes != null && delayMinutes > 1;
  }

  @Override
  public String toString() {
    return String.format("RealTimeBusDto{vehicleId='%s', etaMinutes=%d, distanceMeters=%.0f, scheduled=%s, delay=%s}",
        vehicleId, etaMinutes, distanceMeters, scheduledArrival, 
        delayMinutes != null ? delayMinutes + "min" : "N/A");
  }
}
