package com.bustracker.tracker.service;

import com.bustracker.shared.model.VehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class VehicleStorageService {

  private static final Logger logger = LoggerFactory.getLogger(VehicleStorageService.class);

  // Store vehicles by vehicleId -> VehiclePosition
  private final Map<String, VehiclePosition> vehicles = new ConcurrentHashMap<>();

  // Track when we last updated
  private volatile long lastUpdateTime = System.currentTimeMillis();

  /**
   * Store or update a vehicle position
   */
  public void storeVehiclePosition(VehiclePosition vehiclePosition) {
    vehicles.put(vehiclePosition.getVehicleId(), vehiclePosition);
    lastUpdateTime = System.currentTimeMillis();

    // Log storage stats every 50 vehicles
    if (vehicles.size() % 50 == 0) {
      logger.debug("Stored {} vehicles in memory", vehicles.size());
    }
  }

  /**
   * Get all vehicles
   */
  public List<VehiclePosition> getAllVehicles() {
    return List.copyOf(vehicles.values());
  }

  /**
   * Get vehicles by route
   */
  public List<VehiclePosition> getVehiclesByRoute(String routeId) {
    return vehicles.values().stream()
        .filter(vehicle -> routeId.equals(vehicle.getRouteId()))
        .collect(Collectors.toList());
  }

  /**
   * Get single vehicle by ID
   */
  public VehiclePosition getVehicle(String vehicleId) {
    return vehicles.get(vehicleId);
  }

  /**
   * Get storage statistics
   */
  public StorageStats getStats() {
    return new StorageStats(
        vehicles.size(),
        lastUpdateTime,
        vehicles.values().stream()
            .map(VehiclePosition::getRouteId)
            .distinct()
            .count()
    );
  }

  /**
   * Storage statistics class
   */
  public static class StorageStats {
    private final int totalVehicles;
    private final long lastUpdateTime;
    private final long uniqueRoutes;

    public StorageStats(int totalVehicles, long lastUpdateTime, long uniqueRoutes) {
      this.totalVehicles = totalVehicles;
      this.lastUpdateTime = lastUpdateTime;
      this.uniqueRoutes = uniqueRoutes;
    }

    public int getTotalVehicles() { return totalVehicles; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public long getUniqueRoutes() { return uniqueRoutes; }

    @Override
    public String toString() {
      return String.format("StorageStats{vehicles=%d, routes=%d, lastUpdate=%d}",
          totalVehicles, uniqueRoutes, lastUpdateTime);
    }
  }
}