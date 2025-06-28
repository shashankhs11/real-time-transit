package com.bustracker.tracker.controller;

import com.bustracker.tracker.domain.DirectionName;
import com.bustracker.tracker.domain.Trip;
import com.bustracker.tracker.dto.RouteDto;
import com.bustracker.tracker.dto.DirectionDto;
import com.bustracker.tracker.dto.StopDto;
import com.bustracker.tracker.dto.ArrivalsResponseDto;
import com.bustracker.tracker.dto.RealTimeBusDto;
import com.bustracker.tracker.dto.ScheduledBusDto;
import com.bustracker.tracker.domain.StopTime;
import com.bustracker.tracker.repository.GtfsRepository;
import com.bustracker.tracker.service.VehicleCorrelationService;
import com.bustracker.tracker.service.ScheduledArrivalService;
import com.bustracker.tracker.service.ServiceCalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * REST Controller for route-related endpoints
 * Handles the first step of user flow: route selection
 */
@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*") // Allow frontend access
public class RouteController {

  // Vancouver timezone for GTFS data
  private static final ZoneId VANCOUVER_TIMEZONE = ZoneId.of("America/Vancouver");

  private static final Logger logger = LoggerFactory.getLogger(RouteController.class);

  private final GtfsRepository gtfsRepository;
  private final VehicleCorrelationService vehicleCorrelationService;
  private final ScheduledArrivalService scheduledArrivalService;
  private final ServiceCalendarService serviceCalendarService;

  @Autowired
  public RouteController(GtfsRepository gtfsRepository, VehicleCorrelationService vehicleCorrelationService,
                        ScheduledArrivalService scheduledArrivalService, ServiceCalendarService serviceCalendarService) {
    this.gtfsRepository = gtfsRepository;
    this.vehicleCorrelationService = vehicleCorrelationService;
    this.scheduledArrivalService = scheduledArrivalService;
    this.serviceCalendarService = serviceCalendarService;
  }

  /**
   * GET /api/routes
   * Returns all available bus routes for frontend dropdown
   *
   * Response format:
   * [
   *   {
   *     "routeId": "6635",
   *     "routeShortName": "49",
   *     "routeLongName": "UBC - Metrotown Station"
   *   }
   * ]
   */
  @GetMapping
  public ResponseEntity<List<RouteDto>> getAllRoutes() {
    logger.debug("Fetching all available routes");

    try {
      // Get all routes from repository
      var routes = gtfsRepository.findAllRoutes();

      // Convert domain models to DTOs
      var routeDtos = routes.stream()
          .map(route -> new RouteDto(
              route.getRouteId(),
              route.getRouteShortName(),
              route.getRouteLongName()
          ))
          .sorted((a, b) -> {
            // Sort by route short name numerically where possible
            try {
              int numA = Integer.parseInt(a.getRouteShortName());
              int numB = Integer.parseInt(b.getRouteShortName());
              return Integer.compare(numA, numB);
            } catch (NumberFormatException e) {
              // Fall back to string comparison for non-numeric routes
              return a.getRouteShortName().compareTo(b.getRouteShortName());
            }
          })
          .collect(Collectors.toList());

      logger.debug("Successfully returned {} routes", routeDtos.size());
      return ResponseEntity.ok(routeDtos);

    } catch (Exception e) {
      logger.error("Error fetching routes", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * GET /api/routes/{routeId}/directions
   * Returns available directions for a specific route with friendly names
   *
   * Response format:
   * [
   *   {
   *     "directionId": 0,
   *     "directionName": "Towards Metrotown",
   *     "tripHeadsign": "Metrotown Station"
   *   },
   *   {
   *     "directionId": 1,
   *     "directionName": "Towards UBC",
   *     "tripHeadsign": "UBC"
   *   }
   * ]
   */
  @GetMapping("/{routeId}/directions")
  public ResponseEntity<List<DirectionDto>> getDirectionsForRoute(@PathVariable("routeId") String routeId) {
    logger.debug("Fetching directions for route: {}", routeId);

    try {
      // First, verify the route exists
      var routeOpt = gtfsRepository.findRouteById(routeId);
      if (routeOpt.isEmpty()) {
        logger.warn("Route not found: {}", routeId);
        return ResponseEntity.notFound().build();
      }

      var route = routeOpt.get();
      String routeShortName = route.getRouteShortName();

      // Find all trips for this route to determine available directions
      var allTrips = gtfsRepository.findTripsByRouteId(routeId);

      // Get unique direction IDs
      var uniqueDirectionIds = allTrips.stream()
          .mapToInt(Trip::getDirectionId)
          .distinct()
          .sorted()
          .boxed()
          .toList();

      // Build direction DTOs with friendly names
      var directionDtos = uniqueDirectionIds.stream()
          .map(directionId -> {
            // Try to get friendly direction name from direction_names_exceptions.txt
            var directionNameOpt = gtfsRepository.findDirectionNameByRouteAndDirection(routeShortName, directionId);
            String friendlyName = directionNameOpt
                .map(DirectionName::getDirectionName)
                .orElse("Direction " + directionId);

            // Get a sample trip headsign for this direction
            String tripHeadsign = allTrips.stream()
                .filter(trip -> trip.getDirectionId() == directionId)
                .findFirst()
                .map(Trip::getTripHeadsign)
                .orElse(null);

            return new DirectionDto(directionId, friendlyName, tripHeadsign);
          })
          .collect(Collectors.toList());

      logger.debug("Successfully returned {} directions for route {}", directionDtos.size(), routeShortName);
      return ResponseEntity.ok(directionDtos);

    } catch (Exception e) {
      logger.error("Error fetching directions for route: {}", routeId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * GET /api/routes/{routeId}/directions/{directionId}/stops
   * Returns ordered list of stops for a specific route and direction
   *
   * Response format:
   * [
   *   {
   *     "stopId": "12345",
   *     "stopName": "Metrotown Station Bay 1",
   *     "stopSequence": 1,
   *     "latitude": 49.2268,
   *     "longitude": -123.0118
   *   },
   *   {
   *     "stopId": "12346",
   *     "stopName": "Imperial St @ Kingsway",
   *     "stopSequence": 2,
   *     "latitude": 49.2301,
   *     "longitude": -123.0156
   *   }
   * ]
   */
  @GetMapping("/{routeId}/directions/{directionId}/stops")
  public ResponseEntity<List<StopDto>> getStopsForRouteAndDirection(
      @PathVariable("routeId") String routeId,
      @PathVariable("directionId") int directionId) {

    logger.debug("Fetching stops for route {} direction {}", routeId, directionId);

    try {
      // Verify the route exists
      var routeOpt = gtfsRepository.findRouteById(routeId);
      if (routeOpt.isEmpty()) {
        logger.warn("Route not found: {}", routeId);
        return ResponseEntity.notFound().build();
      }

      // Verify the direction exists for this route
      var trips = gtfsRepository.findTripsByRouteAndDirection(routeId, directionId);
      if (trips.isEmpty()) {
        logger.warn("No trips found for route {} direction {}", routeId, directionId);
        return ResponseEntity.notFound().build();
      }

      // Get stops for this route and direction (already ordered by stop sequence)
      var stops = gtfsRepository.findStopsForRouteAndDirection(routeId, directionId);

      if (stops.isEmpty()) {
        logger.warn("No stops found for route {} direction {}", routeId, directionId);
        return ResponseEntity.notFound().build();
      }

      // Convert to DTOs with sequence numbers
      AtomicInteger sequenceCounter = new AtomicInteger(1);
      var stopDtos = stops.stream()
          .map(stop -> new StopDto(
              stop.getStopId(),
              stop.getStopName(),
              sequenceCounter.getAndIncrement(),
              stop.getStopLat(),
              stop.getStopLon()
          ))
          .collect(Collectors.toList());

      var route = routeOpt.get();
      logger.debug("Successfully returned {} stops for route {} direction {}",
          stopDtos.size(), route.getRouteShortName(), directionId);

      return ResponseEntity.ok(stopDtos);

    } catch (Exception e) {
      logger.error("Error fetching stops for route {} direction {}", routeId, directionId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * GET /api/routes/{routeId}/directions/{directionId}/stops/{stopId}/arrivals
   * THE MAIN ENDPOINT - Returns real-time + scheduled arrivals for a specific stop
   *
   * Response format:
   * {
   *   "route": {
   *     "routeShortName": "49",
   *     "directionName": "Towards UBC"
   *   },
   *   "stop": {
   *     "stopId": "12345",
   *     "stopName": "Metrotown Station Bay 1"
   *   },
   *   "realTimeBuses": [
   *     {
   *       "vehicleId": "18394",
   *       "tripId": "14533627",
   *       "etaMinutes": 4,
   *       "etaSeconds": 243,
   *       "distanceMeters": 850.0,
   *       "currentStatus": "IN_TRANSIT_TO",
   *       "lastUpdated": "2025-06-26T10:30:15Z"
   *     }
   *   ],
   *   "scheduledBuses": [
   *     {
   *       "scheduledArrival": "10:45:00",
   *       "etaMinutes": 15,
   *       "isRealTime": false
   *     }
   *   ]
   * }
   */
  @GetMapping("/{routeId}/directions/{directionId}/stops/{stopId}/arrivals")
  public ResponseEntity<ArrivalsResponseDto> getArrivalsForStop(
      @PathVariable("routeId") String routeId,
      @PathVariable("directionId") int directionId,
      @PathVariable("stopId") String stopId) {

    logger.debug("Fetching arrivals for route {} direction {} stop {}",
        routeId, directionId, stopId);

    try {
      // Step 1: Validate route exists
      var routeOpt = gtfsRepository.findRouteById(routeId);
      if (routeOpt.isEmpty()) {
        logger.warn("Route not found: {}", routeId);
        return ResponseEntity.notFound().build();
      }
      var route = routeOpt.get();

      // Step 2: Validate direction exists for route
      var trips = gtfsRepository.findTripsByRouteAndDirection(routeId, directionId);
      if (trips.isEmpty()) {
        logger.warn("No trips found for route {} direction {}", routeId, directionId);
        return ResponseEntity.notFound().build();
      }

      // Step 3: Validate stop exists
      var stopOpt = gtfsRepository.findStopById(stopId);
      if (stopOpt.isEmpty()) {
        logger.warn("Stop not found: {}", stopId);
        return ResponseEntity.notFound().build();
      }
      var stop = stopOpt.get();

      // Step 3.5: Validate stop serves this route and direction
      var stopsForRoute = gtfsRepository.findStopsForRouteAndDirection(routeId, directionId);
      boolean stopServesRoute = stopsForRoute.stream()
          .anyMatch(routeStop -> routeStop.getStopId().equals(stopId));
      
      if (!stopServesRoute) {
        logger.warn("Stop {} does not serve route {} direction {}", stopId, routeId, directionId);
        return ResponseEntity.notFound().build();
      }

      // Step 4: Get friendly direction name
      var directionNameOpt = gtfsRepository.findDirectionNameByRouteAndDirection(route.getRouteShortName(), directionId);
      String friendlyDirectionName = directionNameOpt
          .map(DirectionName::getDirectionName)
          .orElse("Direction " + directionId);

      // Step 5: Build route and stop info for response
      var routeInfo = new ArrivalsResponseDto.RouteInfoDto(route.getRouteShortName(), friendlyDirectionName);
      var stopInfo = new ArrivalsResponseDto.StopInfoDto(stop.getStopId(), stop.getStopName());

      // Step 6: Get real-time buses from Kafka vehicle positions
      List<RealTimeBusDto> realTimeBuses = getRealTimeBuses(routeId, directionId, stopId);

      // Step 7: Get scheduled buses (simplified for now)
      List<ScheduledBusDto> scheduledBuses = getScheduledArrivals(routeId, directionId, stopId);

      // Step 8: Build response
      var response = new ArrivalsResponseDto(routeInfo, stopInfo, realTimeBuses, scheduledBuses);

      logger.debug("Successfully returned arrivals for stop {} on route {} direction {} - {} real-time, {} scheduled",
          stop.getStopName(), route.getRouteShortName(), directionId,
          realTimeBuses.size(), scheduledBuses.size());

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error("Error fetching arrivals for route {} direction {} stop {}", routeId, directionId, stopId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Get real-time buses approaching the specified stop
   */
  private List<RealTimeBusDto> getRealTimeBuses(String routeId, int directionId, String stopId) {
    try {
      // Use vehicle correlation service to find buses approaching this stop
      var approachingVehicles = vehicleCorrelationService.findVehiclesApproachingStop(routeId, directionId, stopId);

      // Convert to DTOs with scheduled time correlation
      var realTimeBuses = approachingVehicles.stream()
          .map(av -> {
            var vehicle = av.getVehicle();
            var etaResult = av.getEtaResult();

            // Get enhanced vehicle info with scheduled correlation
            var enhancedInfo = scheduledArrivalService.createEnhancedVehicleInfo(
                vehicle, stopId, etaResult.getEtaSeconds());

            // Extract scheduled time information
            var scheduledArrival = enhancedInfo.hasScheduledTime() ? 
                enhancedInfo.getScheduledArrival().getScheduledArrival() : null;
            var delayMinutes = enhancedInfo.hasDelayInfo() ? 
                enhancedInfo.getDelayInfo().getDelayMinutes() : null;
            var delayStatus = enhancedInfo.hasDelayInfo() ? 
                enhancedInfo.getDelayInfo().getStatus().getDisplayName() : null;

            return new RealTimeBusDto(
                vehicle.getVehicleId(),
                vehicle.getTripId(),
                etaResult.getEtaMinutes(),
                etaResult.getEtaSeconds(),
                etaResult.getDistanceMeters(),
                vehicle.getCurrentStatus(),
                av.getCalculatedAt(),
                scheduledArrival,
                delayMinutes,
                delayStatus
            );
          })
          .collect(Collectors.toList());

      logger.debug("Found {} real-time buses approaching stop {}", realTimeBuses.size(), stopId);
      return realTimeBuses;

    } catch (Exception e) {
      logger.error("Error getting real-time buses for stop {}", stopId, e);
      return new ArrayList<>(); // Return empty list on error
    }
  }

  /**
   * Helper method to get scheduled arrivals for a stop using stop_times.txt
   * Filters by active services using calendar.txt + calendar_dates.txt
   */
  private List<ScheduledBusDto> getScheduledArrivals(String routeId, int directionId, String stopId) {
    logger.debug("Getting scheduled arrivals for stop {} in next hour (active services only)", stopId);
    
    try {
      LocalTime currentTime = LocalTime.now(VANCOUVER_TIMEZONE);
      LocalTime endTime = currentTime.plusHours(1); // Show next 1 hour of scheduled arrivals
      
      // Get all stop times for this stop
      List<StopTime> stopTimes = gtfsRepository.findStopTimesByStopId(stopId);
      
      // Step 1: Filter by active services using calendar validation
      List<StopTime> activeStopTimes = serviceCalendarService.filterActiveStopTimes(stopTimes);
      
      logger.debug("Filtered {} stop times to {} active services for stop {}", 
          stopTimes.size(), activeStopTimes.size(), stopId);
      
      // Step 2: Filter for next hour and convert to DTOs
      List<ScheduledBusDto> scheduledBuses = activeStopTimes.stream()
          .filter(stopTime -> {
            LocalTime arrivalTime = stopTime.getArrivalTime();
            return isWithinTimeWindow(arrivalTime, currentTime, endTime);
          })
          .map(stopTime -> {
            LocalTime scheduledTime = stopTime.getArrivalTime();
            int etaMinutes = (int) java.time.Duration.between(currentTime, scheduledTime).toMinutes();
            
            // Handle overnight schedules
            if (etaMinutes < 0) {
              etaMinutes += 24 * 60; // Add 24 hours in minutes
            }
            
            return new ScheduledBusDto(scheduledTime, etaMinutes, false);
          })
          .filter(bus -> bus.getEtaMinutes() >= 0 && bus.getEtaMinutes() <= 60) // Next 1 hour only
          .sorted((ScheduledBusDto a, ScheduledBusDto b) -> Integer.compare(a.getEtaMinutes(), b.getEtaMinutes()))
          .limit(20) // Limit to next 20 scheduled arrivals
          .collect(Collectors.toList());
      
      logger.debug("Found {} active scheduled arrivals at stop {} in next hour", 
          scheduledBuses.size(), stopId);
      
      return scheduledBuses;
      
    } catch (Exception e) {
      logger.error("Error getting scheduled arrivals for stop {}", stopId, e);
      return new ArrayList<>(); // Return empty list on error
    }
  }

  /**
   * Check if a time is within the specified window (handles overnight times)
   */
  private boolean isWithinTimeWindow(LocalTime time, LocalTime fromTime, LocalTime toTime) {
    if (fromTime.isBefore(toTime)) {
      // Normal case: from 08:00 to 09:00
      return !time.isBefore(fromTime) && !time.isAfter(toTime);
    } else {
      // Overnight case: from 23:00 to 01:00
      return !time.isBefore(fromTime) || !time.isAfter(toTime);
    }
  }

  /**
   * GET /api/routes/{routeId}/vehicles
   * Bonus endpoint: Get real-time vehicle statistics for a route
   */
  @GetMapping("/{routeId}/vehicles")
  public ResponseEntity<VehicleCorrelationService.VehicleStats> getVehicleStatsForRoute(@PathVariable("routeId") String routeId) {
    logger.debug("Getting vehicle stats for route {}", routeId);

    try {
      // Verify route exists
      var routeOpt = gtfsRepository.findRouteById(routeId);
      if (routeOpt.isEmpty()) {
        logger.warn("Route not found: {}", routeId);
        return ResponseEntity.notFound().build();
      }

      var stats = vehicleCorrelationService.getVehicleStatsForRoute(routeId);
      var route = routeOpt.get();

      logger.debug("Vehicle stats for route {}: {}", route.getRouteShortName(), stats);
      return ResponseEntity.ok(stats);

    } catch (Exception e) {
      logger.error("Error getting vehicle stats for route {}", routeId, e);
      return ResponseEntity.internalServerError().build();
    }
  }
}