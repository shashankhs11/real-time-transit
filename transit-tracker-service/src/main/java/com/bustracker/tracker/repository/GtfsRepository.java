package com.bustracker.tracker.repository;

import com.bustracker.tracker.domain.Route;
import com.bustracker.tracker.domain.Stop;
import com.bustracker.tracker.domain.Trip;
import com.bustracker.tracker.domain.StopTime;
import com.bustracker.tracker.domain.ShapePoint;
import com.bustracker.tracker.domain.DirectionName;
import com.bustracker.tracker.domain.Calendar;
import com.bustracker.tracker.domain.CalendarDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GTFS static data access
 * Follows Repository pattern for clean separation of data access logic
 */
public interface GtfsRepository {

  // Route operations
  List<Route> findAllRoutes();
  Optional<Route> findRouteById(String routeId);
  List<Route> findRoutesByShortName(String routeShortName);
  List<Route> searchRoutesByShortName(String partialName); // For efficient route search

  // Stop operations
  List<Stop> findAllStops();
  Optional<Stop> findStopById(String stopId);
  List<Stop> findStopsByName(String stopName);
  List<Stop> searchStopsByName(String partialName); // For efficient stop search

  // Trip operations
  List<Trip> findAllTrips();
  Optional<Trip> findTripById(String tripId);
  List<Trip> findTripsByRouteId(String routeId);
  List<Trip> findTripsByRouteAndDirection(String routeId, int directionId);

  // StopTime operations - CRITICAL for user flow
  List<StopTime> findStopTimesByTripId(String tripId);
  List<StopTime> findStopTimesByStopId(String stopId);
  Optional<StopTime> findStopTimeByTripIdAndStopId(String tripId, String stopId);
  List<Stop> findStopsForRouteAndDirection(String routeId, int directionId);
  Optional<Trip> findRepresentativeTripForRouteAndDirection(String routeId, int directionId);

  // Shape operations - for polyline distance calculations
  List<ShapePoint> findShapePointsByShapeId(String shapeId);

  // DirectionName operations - for friendly direction names
  List<DirectionName> findAllDirectionNames();
  Optional<DirectionName> findDirectionNameByRouteAndDirection(String routeShortName, int directionId);
  List<DirectionName> findDirectionNamesByRoute(String routeShortName);

  // Calendar operations - for service schedule validation
  List<Calendar> findAllCalendars();
  Optional<Calendar> findCalendarByServiceId(String serviceId);

  // CalendarDate operations - for service exceptions
  List<CalendarDate> findAllCalendarDates();
  List<CalendarDate> findCalendarDatesByServiceId(String serviceId);
  Optional<CalendarDate> findCalendarDateByServiceIdAndDate(String serviceId, LocalDate date);

  // Data loading operations
  void loadRoutes(List<Route> routes);
  void loadStops(List<Stop> stops);
  void loadTrips(List<Trip> trips);
  void loadStopTimes(List<StopTime> stopTimes);
  void loadShapePoints(List<ShapePoint> shapePoints);
  void loadDirectionNames(List<DirectionName> directionNames);
  void loadCalendars(List<Calendar> calendars);
  void loadCalendarDates(List<CalendarDate> calendarDates);

  // Repository statistics
  RepositoryStats getStats();

  /**
   * Repository statistics for monitoring
   */
  class RepositoryStats {
    private final int routeCount;
    private final int stopCount;
    private final int tripCount;
    private final int stopTimeCount;
    private final int shapePointCount;
    private final int directionNameCount;
    private final int calendarCount;
    private final int calendarDateCount;
    private final long lastLoadTime;

    public RepositoryStats(int routeCount, int stopCount, int tripCount,
        int stopTimeCount, int shapePointCount, int directionNameCount,
        int calendarCount, int calendarDateCount, long lastLoadTime) {
      this.routeCount = routeCount;
      this.stopCount = stopCount;
      this.tripCount = tripCount;
      this.stopTimeCount = stopTimeCount;
      this.shapePointCount = shapePointCount;
      this.directionNameCount = directionNameCount;
      this.calendarCount = calendarCount;
      this.calendarDateCount = calendarDateCount;
      this.lastLoadTime = lastLoadTime;
    }

    public int getRouteCount() { return routeCount; }
    public int getStopCount() { return stopCount; }
    public int getTripCount() { return tripCount; }
    public int getStopTimeCount() { return stopTimeCount; }
    public int getShapePointCount() { return shapePointCount; }
    public int getDirectionNameCount() { return directionNameCount; }
    public int getCalendarCount() { return calendarCount; }
    public int getCalendarDateCount() { return calendarDateCount; }
    public long getLastLoadTime() { return lastLoadTime; }

    @Override
    public String toString() {
      return String.format("RepositoryStats{routes=%d, stops=%d, trips=%d, stopTimes=%d, shapes=%d, directions=%d, calendars=%d, calendarDates=%d, lastLoad=%d}",
          routeCount, stopCount, tripCount, stopTimeCount, shapePointCount, directionNameCount, calendarCount, calendarDateCount, lastLoadTime);
    }
  }
}