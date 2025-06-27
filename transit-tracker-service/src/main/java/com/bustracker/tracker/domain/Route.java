package com.bustracker.tracker.domain;

import java.util.Objects;

/**
 * Domain model representing a bus route from GTFS routes.txt
 * Immutable value object following DDD principles
 */
public class Route {

  private final String routeId;
  private final String routeShortName;  // User-facing route number (e.g., "49", "R4")
  private final String routeLongName;   // Full route description
  private final int routeType;          // Should be 3 for buses

  public Route(String routeId, String routeShortName, String routeLongName, int routeType) {
    this.routeId = Objects.requireNonNull(routeId, "Route ID cannot be null");
    this.routeShortName = Objects.requireNonNull(routeShortName, "Route short name cannot be null");
    this.routeLongName = routeLongName; // Can be null
    this.routeType = routeType;
  }

  public String getRouteId() {
    return routeId;
  }

  public String getRouteShortName() {
    return routeShortName;
  }

  public String getRouteLongName() {
    return routeLongName;
  }

  public int getRouteType() {
    return routeType;
  }

  public boolean isBusRoute() {
    return routeType == 3;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Route route = (Route) o;
    return Objects.equals(routeId, route.routeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(routeId);
  }

  @Override
  public String toString() {
    return String.format("Route{id='%s', shortName='%s', longName='%s'}",
        routeId, routeShortName, routeLongName);
  }
}
