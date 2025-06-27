package com.bustracker.ingestion.service;

import com.bustracker.shared.gtfs.GtfsRealtime;
import com.bustracker.shared.model.VehiclePosition;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GtfsDataTransformer {

  private static final Logger logger = LoggerFactory.getLogger(GtfsDataTransformer.class);

  /**
   * Transform GTFS FeedMessage to list of VehiclePosition objects
   */
  public List<VehiclePosition> transformVehiclePositions(GtfsRealtime.FeedMessage feedMessage) {
    logger.debug("Transforming {} GTFS entities to VehiclePosition objects",
        feedMessage.getEntityCount());

    List<VehiclePosition> vehiclePositions = feedMessage.getEntityList().stream()
        .filter(entity -> entity.hasVehicle() && entity.getVehicle().hasPosition())
        .map(this::transformSingleVehicle)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    logger.info("Successfully transformed {} vehicle positions", vehiclePositions.size());
    return vehiclePositions;
  }

  /**
   * Transform a single GTFS vehicle entity to VehiclePosition
   */
  private VehiclePosition transformSingleVehicle(GtfsRealtime.FeedEntity entity) {
    try {
      GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();

      // Create basic VehiclePosition
      VehiclePosition position = new VehiclePosition();

      // Vehicle ID
      if (vehicle.hasVehicle() && vehicle.getVehicle().hasId()) {
        position.setVehicleId(vehicle.getVehicle().getId());
      }

      // Trip information
      if (vehicle.hasTrip()) {
        GtfsRealtime.TripDescriptor trip = vehicle.getTrip();
        if (trip.hasTripId()) {
          position.setTripId(trip.getTripId());
        }
        if (trip.hasRouteId()) {
          position.setRouteId(trip.getRouteId());
        }
        if (trip.hasDirectionId()) {
          position.setDirectionId(trip.getDirectionId());
        }
      }

      // Position information
      if (vehicle.hasPosition()) {
        GtfsRealtime.Position pos = vehicle.getPosition();
        position.setLatitude(pos.getLatitude());
        position.setLongitude(pos.getLongitude());

        if (pos.hasBearing()) {
          position.setBearing(pos.getBearing());
        }
        if (pos.hasSpeed()) {
          position.setSpeed(pos.getSpeed());
        }
      }

      // Stop and status information
      if (vehicle.hasStopId()) {
        position.setStopId(vehicle.getStopId());
      }

      if (vehicle.hasCurrentStatus()) {
        position.setCurrentStatus(vehicle.getCurrentStatus().name());
      }

      // Timestamp
      if (vehicle.hasTimestamp()) {
        position.setTimestamp(vehicle.getTimestamp());
      } else {
        // Use current time if no timestamp provided
        position.setTimestamp(System.currentTimeMillis() / 1000);
      }

      logger.debug("Transformed vehicle: {}", position);
      return position;

    } catch (Exception e) {
      logger.warn("Failed to transform vehicle entity: {}", entity.getId(), e);
      return null;
    }
  }
}