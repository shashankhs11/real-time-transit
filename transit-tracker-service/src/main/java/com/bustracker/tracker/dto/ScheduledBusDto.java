package com.bustracker.tracker.dto;

import java.time.LocalTime;

/**
 * Data Transfer Object for scheduled bus information
 * Represents buses from static GTFS schedule
 */
public class ScheduledBusDto {

  private LocalTime scheduledArrival;
  private int etaMinutes;
  private boolean isRealTime;

  // Default constructor
  public ScheduledBusDto() {}

  public ScheduledBusDto(LocalTime scheduledArrival, int etaMinutes, boolean isRealTime) {
    this.scheduledArrival = scheduledArrival;
    this.etaMinutes = etaMinutes;
    this.isRealTime = isRealTime;
  }

  public LocalTime getScheduledArrival() {
    return scheduledArrival;
  }

  public void setScheduledArrival(LocalTime scheduledArrival) {
    this.scheduledArrival = scheduledArrival;
  }

  public int getEtaMinutes() {
    return etaMinutes;
  }

  public void setEtaMinutes(int etaMinutes) {
    this.etaMinutes = etaMinutes;
  }

  public boolean isRealTime() {
    return isRealTime;
  }

  public void setRealTime(boolean realTime) {
    isRealTime = realTime;
  }

  @Override
  public String toString() {
    return String.format("ScheduledBusDto{scheduledArrival=%s, etaMinutes=%d, isRealTime=%s}",
        scheduledArrival, etaMinutes, isRealTime);
  }
}