package com.bustracker.tracker.service;

import com.bustracker.shared.model.VehiclePosition;
import com.bustracker.tracker.domain.Stop;
import com.bustracker.tracker.repository.GtfsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for correlating real-time vehicle positions with GTFS static data
 * Finds buses approaching specific stops on specific routes/directions
 */
@Service
public class VehicleCorrelationService {

  private static final Logger logger = LoggerFactory.getLogger(VehicleCorrelationService.class);

  // Maximum distance to consider a bus as "approaching" a stop (in meters)
  private static final double MAX_APPROACH_DISTANCE_M = 3000.0; // 3km

  // Maximum age of vehicle position data to consider (in seconds)
  private static final long MAX_POSITION_AGE_SECONDS = 300; // 5 minutes

  private final GtfsRepository gtfsRepository;
  private final VehicleStorageService vehicleStorageService;
  private final EtaCalculationService etaCalculationService;

  @Autowired
  public VehicleCorrelationService(GtfsRepository gtfsRepository,
      VehicleStorageService vehicleStorageService,
      EtaCalculationService etaCalculationService) {
    this.gtfsRepository = gtfsRepository;
    this.vehicleStorageService = vehicleStorageService;
    this.etaCalculationService = etaCalculationService;
  }

  /**
   * Find all vehicles currently approaching a specific stop on a specific route/direction
   * @param routeId GTFS route ID
   * @param directionId Direction (0 or 1)
   * @param stopId Target stop ID
   * @return List of vehicles with ETA information
   */
  public List<ApproachingVehicle> findVehiclesApproachingStop(String routeId, int directionId, String stopId) {
    logger.debug("🔍 Finding vehicles approaching stop {} on route {} direction {}", stopId, routeId, directionId);

    // Get the target stop
    var stopOpt = gtfsRepository.findStopById(stopId);
    if (stopOpt.isEmpty()) {
      logger.warn("❌ Stop not found: {}", stopId);
      return List.of();
    }
    var targetStop = stopOpt.get();

    // Get all current vehicle positions
    var allVehicles = vehicleStorageService.getAllVehicles();

    // Filter vehicles for this route and direction
    var routeVehicles = allVehicles.stream()
        .filter(vehicle -> routeId.equals(vehicle.getRouteId()))
        .filter(vehicle -> vehicle.getDirectionId() != null && vehicle.getDirectionId() == directionId)
        .filter(this::isVehicleDataFresh)
        .toList();

    logger.debug("📊 Found {} vehicles on route {} direction {} (total vehicles: {})",
        routeVehicles.size(), routeId, directionId, allVehicles.size());

    // Calculate distance and ETA for each vehicle to the target stop
    var approachingVehicles = routeVehicles.stream()
        .map(vehicle -> {
          var etaResult = etaCalculationService.calculateEta(vehicle, targetStop);
          return new ApproachingVehicle(vehicle, targetStop, etaResult);
        })
        .filter(av -> av.getEtaResult().getDistanceMeters() <= MAX_APPROACH_DISTANCE_M)
        .sorted((a, b) -> Double.compare(a.getEtaResult().getDistanceMeters(), b.getEtaResult().getDistanceMeters()))
        .collect(Collectors.toList());

    logger.info("✅ Found {} vehicles approaching stop {} within {}km",
        approachingVehicles.size(), targetStop.getStopName(), MAX_APPROACH_DISTANCE_M / 1000);

    return approachingVehicles;
  }

  /**
   * Check if vehicle position data is fresh enough to use
   * @param vehicle Vehicle position
   * @return true if data is fresh
   */
  private boolean isVehicleDataFresh(VehiclePosition vehicle) {
    long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
    long vehicleTime = vehicle.getTimestamp();
    long ageSeconds = currentTime - vehicleTime;

    boolean isFresh = ageSeconds <= MAX_POSITION_AGE_SECONDS;

    if (!isFresh) {
      logger.debug("🕒 Skipping stale vehicle data: {} is {} seconds old",
          vehicle.getVehicleId(), ageSeconds);
    }

    return isFresh;
  }

  /**
   * Get summary statistics about vehicles on a route
   * @param routeId GTFS route ID
   * @return VehicleStats object
   */
  public VehicleStats getVehicleStatsForRoute(String routeId) {
    var allVehicles = vehicleStorageService.getAllVehicles();

    var routeVehicles = allVehicles.stream()
        .filter(vehicle -> routeId.equals(vehicle.getRouteId()))
        .toList();

    var freshVehicles = routeVehicles.stream()
        .filter(this::isVehicleDataFresh)
        .toList();

    var direction0Count = freshVehicles.stream()
        .filter(v -> v.getDirectionId() != null && v.getDirectionId() == 0)
        .count();

    var direction1Count = freshVehicles.stream()
        .filter(v -> v.getDirectionId() != null && v.getDirectionId() == 1)
        .count();

    return new VehicleStats(routeVehicles.size(), freshVehicles.size(),
        (int) direction0Count, (int) direction1Count);
  }

  /**
   * Container class for vehicles approaching a stop with ETA information
   */
  public static class ApproachingVehicle {
    private final VehiclePosition vehicle;
    private final Stop targetStop;
    private final EtaCalculationService.EtaResult etaResult;
    private final Instant calculatedAt;

    public ApproachingVehicle(VehiclePosition vehicle, Stop targetStop, EtaCalculationService.EtaResult etaResult) {
      this.vehicle = vehicle;
      this.targetStop = targetStop;
      this.etaResult = etaResult;
      this.calculatedAt = Instant.now();
    }

    public VehiclePosition getVehicle() { return vehicle; }
    public Stop getTargetStop() { return targetStop; }
    public EtaCalculationService.EtaResult getEtaResult() { return etaResult; }
    public Instant getCalculatedAt() { return calculatedAt; }

    @Override
    public String toString() {
      return String.format("ApproachingVehicle{vehicleId='%s', tripId='%s', eta=%dmin, distance=%.0fm}",
          vehicle.getVehicleId(), vehicle.getTripId(),
          etaResult.getEtaMinutes(), etaResult.getDistanceMeters());
    }
  }

  /**
   * Statistics class for vehicle counts on a route
   */
  public static class VehicleStats {
    private final int totalVehicles;
    private final int freshVehicles;
    private final int direction0Count;
    private final int direction1Count;

    public VehicleStats(int totalVehicles, int freshVehicles, int direction0Count, int direction1Count) {
      this.totalVehicles = totalVehicles;
      this.freshVehicles = freshVehicles;
      this.direction0Count = direction0Count;
      this.direction1Count = direction1Count;
    }

    public int getTotalVehicles() { return totalVehicles; }
    public int getFreshVehicles() { return freshVehicles; }
    public int getDirection0Count() { return direction0Count; }
    public int getDirection1Count() { return direction1Count; }

    @Override
    public String toString() {
      return String.format("VehicleStats{total=%d, fresh=%d, dir0=%d, dir1=%d}",
          totalVehicles, freshVehicles, direction0Count, direction1Count);
    }
  }
}
