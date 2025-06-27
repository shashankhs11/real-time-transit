package com.bustracker.tracker.dto;

/**
 * Data Transfer Object for Route API responses
 * Clean separation between domain models and API contracts
 */
public class RouteDto {

  private String routeId;
  private String routeShortName;
  private String routeLongName;

  // Default constructor for JSON serialization
  public RouteDto() {}

  public RouteDto(String routeId, String routeShortName, String routeLongName) {
    this.routeId = routeId;
    this.routeShortName = routeShortName;
    this.routeLongName = routeLongName;
  }

  public String getRouteId() {
    return routeId;
  }

  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }

  public String getRouteShortName() {
    return routeShortName;
  }

  public void setRouteShortName(String routeShortName) {
    this.routeShortName = routeShortName;
  }

  public String getRouteLongName() {
    return routeLongName;
  }

  public void setRouteLongName(String routeLongName) {
    this.routeLongName = routeLongName;
  }

  @Override
  public String toString() {
    return String.format("RouteDto{routeId='%s', shortName='%s', longName='%s'}",
        routeId, routeShortName, routeLongName);
  }
}