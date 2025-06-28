package com.bustracker.tracker.repository;

import com.bustracker.tracker.domain.DirectionName;
import com.bustracker.tracker.domain.Route;
import com.bustracker.tracker.domain.Stop;
import com.bustracker.tracker.domain.Trip;
import com.bustracker.tracker.domain.StopTime;
import com.bustracker.tracker.domain.ShapePoint;
import com.bustracker.tracker.domain.Calendar;
import com.bustracker.tracker.domain.CalendarDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of GTFS repository
 * Uses concurrent data structures for thread safety
 * Optimized for read-heavy workloads with indexed lookups
 */
@Repository
public class InMemoryGtfsRepository implements GtfsRepository {

  private static final Logger logger = LoggerFactory.getLogger(InMemoryGtfsRepository.class);

  // Primary storage - thread-safe concurrent maps
  private final Map<String, Route> routesById = new ConcurrentHashMap<>();
  private final Map<String, Stop> stopsById = new ConcurrentHashMap<>();
  private final Map<String, Trip> tripsById = new ConcurrentHashMap<>();
  private final Map<String, List<StopTime>> stopTimesByTripId = new ConcurrentHashMap<>();
  private final Map<String, List<StopTime>> stopTimesByStopId = new ConcurrentHashMap<>();
  private final Map<String, List<ShapePoint>> shapePointsByShapeId = new ConcurrentHashMap<>();
  private final Map<String, DirectionName> directionNamesByCompositeKey = new ConcurrentHashMap<>();
  private final Map<String, Calendar> calendarsById = new ConcurrentHashMap<>();
  private final Map<String, List<CalendarDate>> calendarDatesByServiceId = new ConcurrentHashMap<>();

  // Secondary indexes for efficient lookups
  private final Map<String, List<Route>> routesByShortName = new ConcurrentHashMap<>();
  private final Map<String, List<Trip>> tripsByRouteId = new ConcurrentHashMap<>();
  private final Map<String, Map<Integer, List<Trip>>> tripsByRouteAndDirection = new ConcurrentHashMap<>();
  private final Map<String, List<DirectionName>> directionNamesByRoute = new ConcurrentHashMap<>();

  private volatile long lastLoadTime = 0;

  // Route operations
  @Override
  public List<Route> findAllRoutes() {
    return new ArrayList<>(routesById.values());
  }

  @Override
  public Optional<Route> findRouteById(String routeId) {
    return Optional.ofNullable(routesById.get(routeId));
  }

  @Override
  public List<Route> findRoutesByShortName(String routeShortName) {
    return routesByShortName.getOrDefault(routeShortName, Collections.emptyList());
  }

  @Override
  public List<Route> searchRoutesByShortName(String partialName) {
    if (partialName == null || partialName.trim().isEmpty()) {
      return Collections.emptyList();
    }
    
    String normalizedQuery = partialName.trim().toUpperCase();
    
    return routesById.values().stream()
        .filter(route -> {
          String shortName = route.getRouteShortName().toUpperCase();
          String longName = route.getRouteLongName().toUpperCase();
          return shortName.contains(normalizedQuery) || longName.contains(normalizedQuery);
        })
        .collect(Collectors.toList());
  }

  // Stop operations
  @Override
  public List<Stop> findAllStops() {
    return new ArrayList<>(stopsById.values());
  }

  @Override
  public Optional<Stop> findStopById(String stopId) {
    return Optional.ofNullable(stopsById.get(stopId));
  }

  @Override
  public List<Stop> findStopsByName(String stopName) {
    return stopsById.values().stream()
        .filter(stop -> stop.getStopName().toLowerCase().contains(stopName.toLowerCase()))
        .collect(Collectors.toList());
  }

  @Override
  public List<Stop> searchStopsByName(String partialName) {
    if (partialName == null || partialName.trim().isEmpty()) {
      return Collections.emptyList();
    }
    
    String normalizedQuery = partialName.trim().toLowerCase();
    
    return stopsById.values().stream()
        .filter(stop -> stop.getStopName().toLowerCase().contains(normalizedQuery))
        .limit(50) // Limit to prevent excessive results
        .collect(Collectors.toList());
  }

  // Trip operations
  @Override
  public List<Trip> findAllTrips() {
    return new ArrayList<>(tripsById.values());
  }

  @Override
  public Optional<Trip> findTripById(String tripId) {
    return Optional.ofNullable(tripsById.get(tripId));
  }

  @Override
  public List<Trip> findTripsByRouteId(String routeId) {
    return tripsByRouteId.getOrDefault(routeId, Collections.emptyList());
  }

  @Override
  public List<Trip> findTripsByRouteAndDirection(String routeId, int directionId) {
    return tripsByRouteAndDirection
        .getOrDefault(routeId, Collections.emptyMap())
        .getOrDefault(directionId, Collections.emptyList());
  }

  // StopTime operations - CRITICAL for user flow
  @Override
  public List<StopTime> findStopTimesByTripId(String tripId) {
    return stopTimesByTripId.getOrDefault(tripId, Collections.emptyList());
  }

  @Override
  public List<StopTime> findStopTimesByStopId(String stopId) {
    return stopTimesByStopId.getOrDefault(stopId, Collections.emptyList());
  }

  @Override
  public Optional<StopTime> findStopTimeByTripIdAndStopId(String tripId, String stopId) {
    return findStopTimesByTripId(tripId).stream()
        .filter(stopTime -> stopId.equals(stopTime.getStopId()))
        .findFirst();
  }

  @Override
  public List<Stop> findStopsForRouteAndDirection(String routeId, int directionId) {
    // Get a representative trip for this route and direction
    Optional<Trip> representativeTrip = findRepresentativeTripForRouteAndDirection(routeId, directionId);

    if (representativeTrip.isEmpty()) {
      return Collections.emptyList();
    }

    // Get stop times for this trip, ordered by sequence
    List<StopTime> stopTimes = findStopTimesByTripId(representativeTrip.get().getTripId())
        .stream()
        .sorted(Comparator.comparingInt(StopTime::getStopSequence))
        .toList();

    // Convert stop IDs to Stop objects
    return stopTimes.stream()
        .map(stopTime -> findStopById(stopTime.getStopId()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Trip> findRepresentativeTripForRouteAndDirection(String routeId, int directionId) {
    List<Trip> trips = findTripsByRouteAndDirection(routeId, directionId);

    if (trips.isEmpty()) {
      return Optional.empty();
    }

    // Pick the first trip that has stop times
    return trips.stream()
        .filter(trip -> !findStopTimesByTripId(trip.getTripId()).isEmpty())
        .findFirst()
        .or(() -> trips.stream().findFirst()); // Fallback to any trip
  }

  // Shape operations
  @Override
  public List<ShapePoint> findShapePointsByShapeId(String shapeId) {
    return shapePointsByShapeId.getOrDefault(shapeId, Collections.emptyList())
        .stream()
        .sorted(Comparator.comparingInt(ShapePoint::getShapePtSequence))
        .collect(Collectors.toList());
  }

  // DirectionName operations
  @Override
  public List<DirectionName> findAllDirectionNames() {
    return new ArrayList<>(directionNamesByCompositeKey.values());
  }

  @Override
  public Optional<DirectionName> findDirectionNameByRouteAndDirection(String routeShortName, int directionId) {
    String compositeKey = routeShortName + ":" + directionId;
    return Optional.ofNullable(directionNamesByCompositeKey.get(compositeKey));
  }

  @Override
  public List<DirectionName> findDirectionNamesByRoute(String routeShortName) {
    return directionNamesByRoute.getOrDefault(routeShortName, Collections.emptyList());
  }

  // Calendar operations
  @Override
  public List<Calendar> findAllCalendars() {
    return new ArrayList<>(calendarsById.values());
  }

  @Override
  public Optional<Calendar> findCalendarByServiceId(String serviceId) {
    return Optional.ofNullable(calendarsById.get(serviceId));
  }

  // CalendarDate operations
  @Override
  public List<CalendarDate> findAllCalendarDates() {
    return calendarDatesByServiceId.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @Override
  public List<CalendarDate> findCalendarDatesByServiceId(String serviceId) {
    return calendarDatesByServiceId.getOrDefault(serviceId, Collections.emptyList());
  }

  @Override
  public Optional<CalendarDate> findCalendarDateByServiceIdAndDate(String serviceId, LocalDate date) {
    return findCalendarDatesByServiceId(serviceId).stream()
        .filter(calendarDate -> calendarDate.getDate().equals(date))
        .findFirst();
  }

  // Data loading operations
  @Override
  public void loadRoutes(List<Route> routes) {
    logger.info("📦 Loading {} routes into repository", routes.size());

    routesById.clear();
    routesByShortName.clear();

    for (Route route : routes) {
      // Primary storage
      routesById.put(route.getRouteId(), route);

      // Secondary index by short name
      routesByShortName
          .computeIfAbsent(route.getRouteShortName(), k -> new ArrayList<>())
          .add(route);
    }

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} routes with {} unique short names",
        routes.size(), routesByShortName.size());
  }

  @Override
  public void loadStops(List<Stop> stops) {
    logger.info("📦 Loading {} stops into repository", stops.size());

    stopsById.clear();

    for (Stop stop : stops) {
      stopsById.put(stop.getStopId(), stop);
    }

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} stops", stops.size());
  }

  @Override
  public void loadTrips(List<Trip> trips) {
    logger.info("📦 Loading {} trips into repository", trips.size());

    tripsById.clear();
    tripsByRouteId.clear();
    tripsByRouteAndDirection.clear();

    for (Trip trip : trips) {
      // Primary storage
      tripsById.put(trip.getTripId(), trip);

      // Secondary index by route ID
      tripsByRouteId
          .computeIfAbsent(trip.getRouteId(), k -> new ArrayList<>())
          .add(trip);

      // Secondary index by route and direction
      tripsByRouteAndDirection
          .computeIfAbsent(trip.getRouteId(), k -> new ConcurrentHashMap<>())
          .computeIfAbsent(trip.getDirectionId(), k -> new ArrayList<>())
          .add(trip);
    }

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} trips for {} routes", trips.size(), tripsByRouteId.size());
  }

  @Override
  public void loadStopTimes(List<StopTime> stopTimes) {
    logger.info("📦 Loading {} stop times into repository", stopTimes.size());

    stopTimesByTripId.clear();
    stopTimesByStopId.clear();

    for (StopTime stopTime : stopTimes) {
      // Index by trip ID
      stopTimesByTripId
          .computeIfAbsent(stopTime.getTripId(), k -> new ArrayList<>())
          .add(stopTime);
      
      // Index by stop ID
      stopTimesByStopId
          .computeIfAbsent(stopTime.getStopId(), k -> new ArrayList<>())
          .add(stopTime);
    }

    // Sort stop times by sequence for each trip
    stopTimesByTripId.values().forEach(tripStopTimes ->
        tripStopTimes.sort(Comparator.comparingInt(StopTime::getStopSequence)));

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} stop times for {} trips", stopTimes.size(), stopTimesByTripId.size());
  }

  @Override
  public void loadShapePoints(List<ShapePoint> shapePoints) {
    logger.info("📦 Loading {} shape points into repository", shapePoints.size());

    shapePointsByShapeId.clear();

    for (ShapePoint shapePoint : shapePoints) {
      shapePointsByShapeId
          .computeIfAbsent(shapePoint.getShapeId(), k -> new ArrayList<>())
          .add(shapePoint);
    }

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} shape points for {} shapes", shapePoints.size(), shapePointsByShapeId.size());
  }

  @Override
  public void loadDirectionNames(List<DirectionName> directionNames) {
    logger.info("📦 Loading {} direction names into repository", directionNames.size());

    directionNamesByCompositeKey.clear();
    directionNamesByRoute.clear();

    for (DirectionName directionName : directionNames) {
      // Primary storage by composite key
      directionNamesByCompositeKey.put(directionName.getCompositeKey(), directionName);

      // Secondary index by route name
      directionNamesByRoute
          .computeIfAbsent(directionName.getRouteName(), k -> new ArrayList<>())
          .add(directionName);
    }

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} direction names for {} routes", directionNames.size(), directionNamesByRoute.size());
  }

  @Override
  public void loadCalendars(List<Calendar> calendars) {
    logger.info("📦 Loading {} calendars into repository", calendars.size());

    calendarsById.clear();

    for (Calendar calendar : calendars) {
      calendarsById.put(calendar.getServiceId(), calendar);
    }

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} calendars", calendars.size());
  }

  @Override
  public void loadCalendarDates(List<CalendarDate> calendarDates) {
    logger.info("📦 Loading {} calendar dates into repository", calendarDates.size());

    calendarDatesByServiceId.clear();

    for (CalendarDate calendarDate : calendarDates) {
      calendarDatesByServiceId
          .computeIfAbsent(calendarDate.getServiceId(), k -> new ArrayList<>())
          .add(calendarDate);
    }

    lastLoadTime = System.currentTimeMillis();
    logger.info("✅ Loaded {} calendar dates for {} services", calendarDates.size(), calendarDatesByServiceId.size());
  }

  @Override
  public RepositoryStats getStats() {
    return new RepositoryStats(
        routesById.size(),
        stopsById.size(),
        tripsById.size(),
        stopTimesByTripId.values().stream().mapToInt(List::size).sum(),
        shapePointsByShapeId.values().stream().mapToInt(List::size).sum(),
        directionNamesByCompositeKey.size(),
        calendarsById.size(),
        calendarDatesByServiceId.values().stream().mapToInt(List::size).sum(),
        lastLoadTime
    );
  }
}