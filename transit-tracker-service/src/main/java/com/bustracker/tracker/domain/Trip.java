package com.bustracker.tracker.domain;

import java.util.Objects;

/**
 * Domain model representing a trip from GTFS trips.txt
 * Links route, direction, and shape information
 */
public class Trip {

  private final String tripId;
  private final String routeId;
  private final String shapeId;
  private final int directionId;
  private final String tripHeadsign;

  public Trip(String tripId, String routeId, String shapeId, int directionId, String tripHeadsign) {
    this.tripId = Objects.requireNonNull(tripId, "Trip ID cannot be null");
    this.routeId = Objects.requireNonNull(routeId, "Route ID cannot be null");
    this.shapeId = shapeId; // Can be null
    this.directionId = directionId;
    this.tripHeadsign = tripHeadsign; // Can be null

    // Validate direction ID
    if (directionId < 0 || directionId > 1) {
      throw new IllegalArgumentException("Direction ID must be 0 or 1, got: " + directionId);
    }
  }

  public String getTripId() {
    return tripId;
  }

  public String getRouteId() {
    return routeId;
  }

  public String getShapeId() {
    return shapeId;
  }

  public int getDirectionId() {
    return directionId;
  }

  public String getTripHeadsign() {
    return tripHeadsign;
  }

  public boolean hasShape() {
    return shapeId != null && !shapeId.trim().isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Trip trip = (Trip) o;
    return Objects.equals(tripId, trip.tripId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tripId);
  }

  @Override
  public String toString() {
    return String.format("Trip{id='%s', routeId='%s', directionId=%d, headsign='%s'}",
        tripId, routeId, directionId, tripHeadsign);
  }
}