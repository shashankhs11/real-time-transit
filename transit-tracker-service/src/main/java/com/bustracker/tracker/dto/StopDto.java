package com.bustracker.tracker.dto;

/**
 * Data Transfer Object for Stop API responses
 * Represents ordered stops along a route direction
 */
public class StopDto {

  private String stopId;
  private String stopName;
  private int stopSequence;
  private double latitude;
  private double longitude;

  // Default constructor for JSON serialization
  public StopDto() {}

  public StopDto(String stopId, String stopName, int stopSequence, double latitude, double longitude) {
    this.stopId = stopId;
    this.stopName = stopName;
    this.stopSequence = stopSequence;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String getStopId() {
    return stopId;
  }

  public void setStopId(String stopId) {
    this.stopId = stopId;
  }

  public String getStopName() {
    return stopName;
  }

  public void setStopName(String stopName) {
    this.stopName = stopName;
  }

  public int getStopSequence() {
    return stopSequence;
  }

  public void setStopSequence(int stopSequence) {
    this.stopSequence = stopSequence;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  @Override
  public String toString() {
    return String.format("StopDto{stopId='%s', name='%s', sequence=%d, lat=%.6f, lng=%.6f}",
        stopId, stopName, stopSequence, latitude, longitude);
  }
}
