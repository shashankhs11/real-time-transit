package com.bustracker.tracker.domain;

import java.util.Objects;

/**
 * Domain model representing direction names from GTFS direction_names_exceptions.txt
 * Maps route + direction to user-friendly direction names
 * TransLink-specific file format: route_name,direction_id,direction_name,direction_do
 */
public class DirectionName {

  private final String routeName;     // Maps to route_short_name (e.g., "49")
  private final int directionId;      // 0 or 1
  private final String directionName; // User-friendly name (e.g., "Towards UBC")
  private final String directionDo;   // Additional field from TransLink data

  public DirectionName(String routeName, int directionId, String directionName, String directionDo) {
    this.routeName = Objects.requireNonNull(routeName, "Route name cannot be null");
    this.directionId = directionId;
    this.directionName = Objects.requireNonNull(directionName, "Direction name cannot be null");
    this.directionDo = directionDo; // Can be null

    // Validate direction ID
    if (directionId < 0 || directionId > 1) {
      throw new IllegalArgumentException("Direction ID must be 0 or 1, got: " + directionId);
    }
  }

  public String getRouteName() {
    return routeName;
  }

  public int getDirectionId() {
    return directionId;
  }

  public String getDirectionName() {
    return directionName;
  }

  public String getDirectionDo() {
    return directionDo;
  }

  /**
   * Create a composite key for lookups
   */
  public String getCompositeKey() {
    return routeName + ":" + directionId;
  }

  /**
   * Check if this direction name matches a route short name and direction
   */
  public boolean matches(String routeShortName, int direction) {
    return this.routeName.equals(routeShortName) && this.directionId == direction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DirectionName that = (DirectionName) o;
    return directionId == that.directionId &&
        Objects.equals(routeName, that.routeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(routeName, directionId);
  }

  @Override
  public String toString() {
    return String.format("DirectionName{route='%s', directionId=%d, name='%s'}",
        routeName, directionId, directionName);
  }
}