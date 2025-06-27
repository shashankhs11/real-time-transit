package com.bustracker.tracker.dto;

import java.util.List;

/**
 * Data Transfer Object for the complete arrivals response
 * Combines route/stop info with real-time and scheduled buses
 */
public class ArrivalsResponseDto {

  private RouteInfoDto route;
  private StopInfoDto stop;
  private List<RealTimeBusDto> realTimeBuses;
  private List<ScheduledBusDto> scheduledBuses;

  // Default constructor
  public ArrivalsResponseDto() {}

  public ArrivalsResponseDto(RouteInfoDto route, StopInfoDto stop,
      List<RealTimeBusDto> realTimeBuses, List<ScheduledBusDto> scheduledBuses) {
    this.route = route;
    this.stop = stop;
    this.realTimeBuses = realTimeBuses;
    this.scheduledBuses = scheduledBuses;
  }

  public RouteInfoDto getRoute() {
    return route;
  }

  public void setRoute(RouteInfoDto route) {
    this.route = route;
  }

  public StopInfoDto getStop() {
    return stop;
  }

  public void setStop(StopInfoDto stop) {
    this.stop = stop;
  }

  public List<RealTimeBusDto> getRealTimeBuses() {
    return realTimeBuses;
  }

  public void setRealTimeBuses(List<RealTimeBusDto> realTimeBuses) {
    this.realTimeBuses = realTimeBuses;
  }

  public List<ScheduledBusDto> getScheduledBuses() {
    return scheduledBuses;
  }

  public void setScheduledBuses(List<ScheduledBusDto> scheduledBuses) {
    this.scheduledBuses = scheduledBuses;
  }

  /**
   * Nested DTO for route information in arrivals response
   */
  public static class RouteInfoDto {
    private String routeShortName;
    private String directionName;

    public RouteInfoDto() {}

    public RouteInfoDto(String routeShortName, String directionName) {
      this.routeShortName = routeShortName;
      this.directionName = directionName;
    }

    public String getRouteShortName() { return routeShortName; }
    public void setRouteShortName(String routeShortName) { this.routeShortName = routeShortName; }
    public String getDirectionName() { return directionName; }
    public void setDirectionName(String directionName) { this.directionName = directionName; }
  }

  /**
   * Nested DTO for stop information in arrivals response
   */
  public static class StopInfoDto {
    private String stopId;
    private String stopName;

    public StopInfoDto() {}

    public StopInfoDto(String stopId, String stopName) {
      this.stopId = stopId;
      this.stopName = stopName;
    }

    public String getStopId() { return stopId; }
    public void setStopId(String stopId) { this.stopId = stopId; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
  }
}