package com.bustracker.tracker.service;

import com.bustracker.shared.model.VehiclePosition;
import com.bustracker.tracker.domain.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for calculating ETAs and distances for real-time bus tracking
 * Uses Haversine formula for distance calculation and simple speed assumptions for ETA
 */
@Service
public class EtaCalculationService {

  private static final Logger logger = LoggerFactory.getLogger(EtaCalculationService.class);

  // Average bus speed assumptions for Vancouver (in km/h)
  private static final double AVERAGE_SPEED_KMH = 25.0; // Conservative city speed
  private static final double AVERAGE_SPEED_MS = AVERAGE_SPEED_KMH / 3.6; // Convert to m/s

  // Earth's radius in meters for Haversine calculation
  private static final double EARTH_RADIUS_M = 6371000.0;

  /**
   * Calculate straight-line distance between vehicle and stop using Haversine formula
   * @param vehiclePosition Current vehicle position
   * @param targetStop Target stop
   * @return Distance in meters
   */
  public double calculateDistance(VehiclePosition vehiclePosition, Stop targetStop) {
    return calculateDistance(
        vehiclePosition.getLatitude(),
        vehiclePosition.getLongitude(),
        targetStop.getStopLat(),
        targetStop.getStopLon()
    );
  }

  /**
   * Calculate distance between two geographic points using Haversine formula
   * @param lat1 Latitude of first point
   * @param lon1 Longitude of first point
   * @param lat2 Latitude of second point
   * @param lon2 Longitude of second point
   * @return Distance in meters
   */
  public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = EARTH_RADIUS_M * c;

    logger.debug("Calculated distance: {:.0f}m between ({:.6f},{:.6f}) and ({:.6f},{:.6f})",
        distance, lat1, lon1, lat2, lon2);

    return distance;
  }

  /**
   * Calculate ETA in seconds based on distance and average speed
   * @param distanceMeters Distance to travel in meters
   * @return ETA in seconds
   */
  public int calculateEtaSeconds(double distanceMeters) {
    if (distanceMeters <= 0) {
      return 0;
    }

    // Simple calculation: time = distance / speed
    double etaSeconds = distanceMeters / AVERAGE_SPEED_MS;

    // Add buffer time for stops, traffic lights, etc. (20% buffer)
    etaSeconds *= 1.2;

    // Minimum ETA of 30 seconds (bus could be very close)
    etaSeconds = Math.max(etaSeconds, 30);

    logger.debug("Calculated ETA: {:.0f} seconds for distance {:.0f}m", etaSeconds, distanceMeters);

    return (int) Math.round(etaSeconds);
  }

  /**
   * Calculate ETA in minutes (rounded)
   * @param etaSeconds ETA in seconds
   * @return ETA in minutes
   */
  public int calculateEtaMinutes(int etaSeconds) {
    return (int) Math.round(etaSeconds / 60.0);
  }

  /**
   * Calculate both distance and ETA for a vehicle approaching a stop
   * @param vehiclePosition Current vehicle position
   * @param targetStop Target stop
   * @return EtaResult with distance and time information
   */
  public EtaResult calculateEta(VehiclePosition vehiclePosition, Stop targetStop) {
    double distance = calculateDistance(vehiclePosition, targetStop);
    int etaSeconds = calculateEtaSeconds(distance);
    int etaMinutes = calculateEtaMinutes(etaSeconds);

    logger.debug("ETA calculation for vehicle {} to stop {}: {}m, {}min {}sec",
        vehiclePosition.getVehicleId(), targetStop.getStopId(),
        Math.round(distance), etaMinutes, etaSeconds % 60);

    return new EtaResult(distance, etaSeconds, etaMinutes);
  }

  /**
   * Result class for ETA calculations
   */
  public static class EtaResult {
    private final double distanceMeters;
    private final int etaSeconds;
    private final int etaMinutes;

    public EtaResult(double distanceMeters, int etaSeconds, int etaMinutes) {
      this.distanceMeters = distanceMeters;
      this.etaSeconds = etaSeconds;
      this.etaMinutes = etaMinutes;
    }

    public double getDistanceMeters() { return distanceMeters; }
    public int getEtaSeconds() { return etaSeconds; }
    public int getEtaMinutes() { return etaMinutes; }

    @Override
    public String toString() {
      return String.format("EtaResult{distance=%.0fm, eta=%dmin %dsec}",
          distanceMeters, etaMinutes, etaSeconds % 60);
    }
  }
}