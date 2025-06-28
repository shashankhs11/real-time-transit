package com.bustracker.tracker.service;

import com.bustracker.shared.model.VehiclePosition;
import com.bustracker.tracker.domain.StopTime;
import com.bustracker.tracker.repository.GtfsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduledArrivalService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledArrivalService.class);
    
    private final GtfsRepository gtfsRepository;

    @Autowired
    public ScheduledArrivalService(GtfsRepository gtfsRepository) {
        this.gtfsRepository = gtfsRepository;
    }

    /**
     * Get scheduled arrival time for a specific trip at a specific stop
     * @param tripId GTFS trip ID
     * @param stopId GTFS stop ID
     * @return ScheduledArrival info or empty if not found
     */
    public Optional<ScheduledArrival> getScheduledArrival(String tripId, String stopId) {
        logger.debug("Getting scheduled arrival for trip {} at stop {}", tripId, stopId);
        
        Optional<StopTime> stopTimeOpt = gtfsRepository.findStopTimeByTripIdAndStopId(tripId, stopId);
        
        if (stopTimeOpt.isEmpty()) {
            logger.warn("No scheduled time found for trip {} at stop {} - checking if trip/stop exist", tripId, stopId);
            
            // Debug: Check if trip exists
            var tripExists = gtfsRepository.findTripById(tripId);
            logger.debug("Trip {} exists: {}", tripId, tripExists.isPresent());
            
            // Debug: Check if stop exists  
            var stopExists = gtfsRepository.findStopById(stopId);
            logger.debug("Stop {} exists: {}", stopId, stopExists.isPresent());
            
            // Debug: Check what stop times exist for this trip
            var tripStopTimes = gtfsRepository.findStopTimesByTripId(tripId);
            logger.debug("Trip {} has {} stop times", tripId, tripStopTimes.size());
            if (!tripStopTimes.isEmpty()) {
                logger.debug("First few stops for trip {}: {}", tripId, 
                    tripStopTimes.stream().limit(3).map(StopTime::getStopId).toList());
            }
            
            return Optional.empty();
        }
        
        StopTime stopTime = stopTimeOpt.get();
        logger.debug("Found scheduled arrival for trip {} at stop {}: {}", 
            tripId, stopId, stopTime.getArrivalTime());
        
        return Optional.of(new ScheduledArrival(
            tripId,
            stopId,
            stopTime.getArrivalTime(),
            stopTime.getStopSequence()
        ));
    }

    /**
     * Calculate delay between scheduled and real-time arrival
     * @param scheduledArrival Scheduled arrival information
     * @param realTimeEtaSeconds Current real-time ETA in seconds
     * @return DelayInfo with delay calculation
     */
    public DelayInfo calculateDelay(ScheduledArrival scheduledArrival, int realTimeEtaSeconds) {
        LocalTime now = LocalTime.now();
        LocalTime scheduledTime = scheduledArrival.getScheduledArrival();
        LocalTime predictedArrival = now.plusSeconds(realTimeEtaSeconds);
        
        // Calculate delay in minutes (positive = late, negative = early)
        long delayMinutes = ChronoUnit.MINUTES.between(scheduledTime, predictedArrival);
        
        // Determine delay status
        DelayStatus status;
        if (Math.abs(delayMinutes) <= 1) {
            status = DelayStatus.ON_TIME;
        } else if (delayMinutes > 1) {
            status = DelayStatus.DELAYED;
        } else {
            status = DelayStatus.EARLY;
        }
        
        logger.debug("Delay calculation: scheduled={}, predicted={}, delay={}min, status={}", 
            scheduledTime, predictedArrival, delayMinutes, status);
        
        return new DelayInfo(
            scheduledTime,
            predictedArrival,
            (int) delayMinutes,
            status
        );
    }

    /**
     * Get all scheduled arrivals for a specific stop within a time window
     * @param stopId GTFS stop ID
     * @param fromTime Start of time window
     * @param toTime End of time window
     * @return List of scheduled arrivals within the time window
     */
    public List<ScheduledArrival> getScheduledArrivalsForStop(String stopId, LocalTime fromTime, LocalTime toTime) {
        logger.debug("Getting scheduled arrivals for stop {} between {} and {}", stopId, fromTime, toTime);
        
        List<StopTime> stopTimes = gtfsRepository.findStopTimesByStopId(stopId);
        
        return stopTimes.stream()
            .filter(stopTime -> isWithinTimeWindow(stopTime.getArrivalTime(), fromTime, toTime))
            .map(stopTime -> new ScheduledArrival(
                stopTime.getTripId(),
                stopTime.getStopId(),
                stopTime.getArrivalTime(),
                stopTime.getStopSequence()
            ))
            .sorted((a, b) -> a.getScheduledArrival().compareTo(b.getScheduledArrival()))
            .collect(Collectors.toList());
    }

    /**
     * Create enhanced vehicle information with scheduled time correlation
     * @param vehicle Real-time vehicle position
     * @param stopId Target stop ID
     * @param etaSeconds Real-time ETA in seconds
     * @return Enhanced vehicle info with scheduled correlation
     */
    public EnhancedVehicleInfo createEnhancedVehicleInfo(VehiclePosition vehicle, String stopId, int etaSeconds) {
        logger.debug("Creating enhanced vehicle info for vehicle {} to stop {}", 
            vehicle.getVehicleId(), stopId);
        
        Optional<ScheduledArrival> scheduledArrival = Optional.empty();
        DelayInfo delayInfo = null;
        
        // Try to get scheduled arrival if trip ID is available
        if (vehicle.getTripId() != null) {
            scheduledArrival = getScheduledArrival(vehicle.getTripId(), stopId);
            
            if (scheduledArrival.isPresent()) {
                delayInfo = calculateDelay(scheduledArrival.get(), etaSeconds);
                logger.debug("Vehicle {} has scheduled correlation: {}", 
                    vehicle.getVehicleId(), delayInfo);
            } else {
                logger.debug("No scheduled time found for vehicle {} trip {} at stop {}", 
                    vehicle.getVehicleId(), vehicle.getTripId(), stopId);
            }
        } else {
            logger.debug("No trip ID available for vehicle {}", vehicle.getVehicleId());
        }
        
        return new EnhancedVehicleInfo(
            vehicle,
            scheduledArrival.orElse(null),
            delayInfo
        );
    }

    /**
     * Check if a time is within the specified window
     */
    private boolean isWithinTimeWindow(LocalTime time, LocalTime fromTime, LocalTime toTime) {
        if (fromTime.isBefore(toTime)) {
            // Normal case: from 08:00 to 10:00
            return !time.isBefore(fromTime) && !time.isAfter(toTime);
        } else {
            // Overnight case: from 23:00 to 01:00
            return !time.isBefore(fromTime) || !time.isAfter(toTime);
        }
    }

    /**
     * Scheduled arrival information
     */
    public static class ScheduledArrival {
        private final String tripId;
        private final String stopId;
        private final LocalTime scheduledArrival;
        private final int stopSequence;

        public ScheduledArrival(String tripId, String stopId, LocalTime scheduledArrival, int stopSequence) {
            this.tripId = tripId;
            this.stopId = stopId;
            this.scheduledArrival = scheduledArrival;
            this.stopSequence = stopSequence;
        }

        public String getTripId() { return tripId; }
        public String getStopId() { return stopId; }
        public LocalTime getScheduledArrival() { return scheduledArrival; }
        public int getStopSequence() { return stopSequence; }

        @Override
        public String toString() {
            return String.format("ScheduledArrival{trip='%s', stop='%s', time=%s, seq=%d}",
                tripId, stopId, scheduledArrival, stopSequence);
        }
    }

    /**
     * Delay information for real-time vs scheduled comparison
     */
    public static class DelayInfo {
        private final LocalTime scheduledTime;
        private final LocalTime predictedTime;
        private final int delayMinutes;
        private final DelayStatus status;

        public DelayInfo(LocalTime scheduledTime, LocalTime predictedTime, int delayMinutes, DelayStatus status) {
            this.scheduledTime = scheduledTime;
            this.predictedTime = predictedTime;
            this.delayMinutes = delayMinutes;
            this.status = status;
        }

        public LocalTime getScheduledTime() { return scheduledTime; }
        public LocalTime getPredictedTime() { return predictedTime; }
        public int getDelayMinutes() { return delayMinutes; }
        public DelayStatus getStatus() { return status; }
        public boolean isDelayed() { return status == DelayStatus.DELAYED; }

        @Override
        public String toString() {
            return String.format("DelayInfo{scheduled=%s, predicted=%s, delay=%dmin, status=%s}",
                scheduledTime, predictedTime, delayMinutes, status);
        }
    }

    /**
     * Enhanced vehicle information with scheduled correlation
     */
    public static class EnhancedVehicleInfo {
        private final VehiclePosition vehicle;
        private final ScheduledArrival scheduledArrival;
        private final DelayInfo delayInfo;

        public EnhancedVehicleInfo(VehiclePosition vehicle, ScheduledArrival scheduledArrival, DelayInfo delayInfo) {
            this.vehicle = vehicle;
            this.scheduledArrival = scheduledArrival;
            this.delayInfo = delayInfo;
        }

        public VehiclePosition getVehicle() { return vehicle; }
        public ScheduledArrival getScheduledArrival() { return scheduledArrival; }
        public DelayInfo getDelayInfo() { return delayInfo; }
        public boolean hasScheduledTime() { return scheduledArrival != null; }
        public boolean hasDelayInfo() { return delayInfo != null; }

        @Override
        public String toString() {
            return String.format("EnhancedVehicleInfo{vehicle='%s', hasScheduled=%s, delay=%s}",
                vehicle.getVehicleId(), hasScheduledTime(), delayInfo != null ? delayInfo.getDelayMinutes() + "min" : "N/A");
        }
    }

    /**
     * Delay status enumeration
     */
    public enum DelayStatus {
        EARLY("Early"),
        ON_TIME("On Time"),
        DELAYED("Delayed");

        private final String displayName;

        DelayStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}