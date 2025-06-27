package com.bustracker.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Simplified vehicle position model for our MVP
 * Extracted from GTFS protobuf data
 */
public class VehiclePosition {

  @JsonProperty("vehicleId")
  private String vehicleId;

  @JsonProperty("tripId")
  private String tripId;

  @JsonProperty("routeId")
  private String routeId;

  @JsonProperty("latitude")
  private double latitude;

  @JsonProperty("longitude")
  private double longitude;

  @JsonProperty("bearing")
  private Float bearing;

  @JsonProperty("speed")
  private Float speed;

  @JsonProperty("stopId")
  private String stopId;

  @JsonProperty("currentStatus")
  private String currentStatus;

  @JsonProperty("timestamp")
  private long timestamp;

  @JsonProperty("directionId")
  private Integer directionId;

  // Default constructor
  public VehiclePosition() {}

  // Constructor with essential fields
  public VehiclePosition(String vehicleId, String tripId, String routeId,
      double latitude, double longitude, long timestamp) {
    this.vehicleId = vehicleId;
    this.tripId = tripId;
    this.routeId = routeId;
    this.latitude = latitude;
    this.longitude = longitude;
    this.timestamp = timestamp;
  }

  // Getters and Setters
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

  public String getRouteId() {
    return routeId;
  }

  public void setRouteId(String routeId) {
    this.routeId = routeId;
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

  public Float getBearing() {
    return bearing;
  }

  public void setBearing(Float bearing) {
    this.bearing = bearing;
  }

  public Float getSpeed() {
    return speed;
  }

  public void setSpeed(Float speed) {
    this.speed = speed;
  }

  public String getStopId() {
    return stopId;
  }

  public void setStopId(String stopId) {
    this.stopId = stopId;
  }

  public String getCurrentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(String currentStatus) {
    this.currentStatus = currentStatus;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public Integer getDirectionId() {
    return directionId;
  }

  public void setDirectionId(Integer directionId) {
    this.directionId = directionId;
  }

  @Override
  public String toString() {
    return String.format("VehiclePosition{vehicleId='%s', routeId='%s', lat=%.6f, lng=%.6f, status='%s'}",
        vehicleId, routeId, latitude, longitude, currentStatus);
  }

  /**
   * Get timestamp as Instant for easier time handling
   * This method is excluded from JSON serialization to avoid Jackson issues
   */
  @JsonIgnore
  public Instant getTimestampAsInstant() {
    return Instant.ofEpochSecond(timestamp);
  }
}