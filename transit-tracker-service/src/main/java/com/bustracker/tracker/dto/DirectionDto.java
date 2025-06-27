package com.bustracker.tracker.dto;

/**
 * Data Transfer Object for Direction API responses
 * Represents available directions for a route
 */
public class DirectionDto {

  private int directionId;
  private String directionName;
  private String tripHeadsign;

  // Default constructor for JSON serialization
  public DirectionDto() {}

  public DirectionDto(int directionId, String directionName, String tripHeadsign) {
    this.directionId = directionId;
    this.directionName = directionName;
    this.tripHeadsign = tripHeadsign;
  }

  public int getDirectionId() {
    return directionId;
  }

  public void setDirectionId(int directionId) {
    this.directionId = directionId;
  }

  public String getDirectionName() {
    return directionName;
  }

  public void setDirectionName(String directionName) {
    this.directionName = directionName;
  }

  public String getTripHeadsign() {
    return tripHeadsign;
  }

  public void setTripHeadsign(String tripHeadsign) {
    this.tripHeadsign = tripHeadsign;
  }

  @Override
  public String toString() {
    return String.format("DirectionDto{directionId=%d, name='%s', headsign='%s'}",
        directionId, directionName, tripHeadsign);
  }
}