package com.bustracker.tracker.service;

import com.bustracker.tracker.domain.Calendar;
import com.bustracker.tracker.domain.CalendarDate;
import com.bustracker.tracker.domain.StopTime;
import com.bustracker.tracker.domain.Trip;
import com.bustracker.tracker.repository.GtfsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServiceCalendarService {

    // Vancouver timezone for GTFS data
    private static final ZoneId VANCOUVER_TIMEZONE = ZoneId.of("America/Vancouver");
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceCalendarService.class);
    
    private final GtfsRepository gtfsRepository;

    @Autowired
    public ServiceCalendarService(GtfsRepository gtfsRepository) {
        this.gtfsRepository = gtfsRepository;
    }

    /**
     * Check if a service is active on a specific date
     * Combines calendar.txt regular schedule with calendar_dates.txt exceptions
     * @param serviceId GTFS service ID
     * @param date Date to check
     * @return true if service is active on this date
     */
    public boolean isServiceActiveOnDate(String serviceId, LocalDate date) {
        logger.debug("Checking if service {} is active on {}", serviceId, date);

        // Step 1: Check calendar_dates.txt for exceptions FIRST (highest priority)
        Optional<CalendarDate> exception = gtfsRepository.findCalendarDateByServiceIdAndDate(serviceId, date);
        if (exception.isPresent()) {
            boolean isActive = exception.get().isServiceAdded();
            logger.debug("Service {} has exception on {}: {} (type={})", 
                serviceId, date, isActive ? "ACTIVE" : "INACTIVE", exception.get().getExceptionType());
            return isActive;
        }

        // Step 2: Fall back to regular calendar.txt schedule
        Optional<Calendar> calendar = gtfsRepository.findCalendarByServiceId(serviceId);
        if (calendar.isPresent()) {
            boolean isActive = calendar.get().isActiveOnDate(date);
            logger.debug("Service {} regular schedule on {}: {}", serviceId, date, isActive ? "ACTIVE" : "INACTIVE");
            return isActive;
        }

        // Step 3: No calendar data found
        logger.debug("No calendar data found for service {}, assuming INACTIVE", serviceId);
        return false;
    }

    /**
     * Check if a service is active today
     * @param serviceId GTFS service ID
     * @return true if service is active today
     */
    public boolean isServiceActiveToday(String serviceId) {
        return isServiceActiveOnDate(serviceId, LocalDate.now(VANCOUVER_TIMEZONE));
    }

    /**
     * Filter a list of trip IDs to only include trips with active services today
     * @param tripIds List of trip IDs to filter
     * @return List of trip IDs with active services
     */
    public List<String> filterActiveTripIds(List<String> tripIds) {
        LocalDate today = LocalDate.now(VANCOUVER_TIMEZONE);
        
        return tripIds.stream()
            .filter(tripId -> {
                Optional<Trip> tripOpt = gtfsRepository.findTripById(tripId);
                if (tripOpt.isEmpty()) {
                    logger.debug("Trip {} not found, excluding from active trips", tripId);
                    return false;
                }
                
                String serviceId = tripOpt.get().getServiceId();
                boolean isActive = isServiceActiveOnDate(serviceId, today);
                
                if (!isActive) {
                    logger.debug("Trip {} service {} is not active today, excluding", tripId, serviceId);
                }
                
                return isActive;
            })
            .collect(Collectors.toList());
    }

    /**
     * Filter stop times to only include those with active services today
     * @param stopTimes List of stop times to filter
     * @return List of stop times with active services
     */
    public List<StopTime> filterActiveStopTimes(List<StopTime> stopTimes) {
        LocalDate today = LocalDate.now(VANCOUVER_TIMEZONE);
        
        return stopTimes.stream()
            .filter(stopTime -> {
                Optional<Trip> tripOpt = gtfsRepository.findTripById(stopTime.getTripId());
                if (tripOpt.isEmpty()) {
                    logger.debug("Trip {} not found for stop time, excluding", stopTime.getTripId());
                    return false;
                }
                
                String serviceId = tripOpt.get().getServiceId();
                boolean isActive = isServiceActiveOnDate(serviceId, today);
                
                if (!isActive) {
                    logger.debug("Stop time for trip {} service {} is not active today, excluding", 
                        stopTime.getTripId(), serviceId);
                }
                
                return isActive;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get service validation statistics
     * @return ServiceStats with active/inactive service counts
     */
    public ServiceStats getServiceStats() {
        LocalDate today = LocalDate.now(VANCOUVER_TIMEZONE);
        
        List<Calendar> allCalendars = gtfsRepository.findAllCalendars();
        
        int totalServices = allCalendars.size();
        int activeServices = 0;
        int inactiveServices = 0;
        
        for (Calendar calendar : allCalendars) {
            if (isServiceActiveOnDate(calendar.getServiceId(), today)) {
                activeServices++;
            } else {
                inactiveServices++;
            }
        }
        
        List<CalendarDate> todayExceptions = gtfsRepository.findAllCalendarDates().stream()
            .filter(cd -> cd.getDate().equals(today))
            .collect(Collectors.toList());
        
        int exceptionsToday = todayExceptions.size();
        
        return new ServiceStats(totalServices, activeServices, inactiveServices, exceptionsToday);
    }

    /**
     * Service statistics for monitoring
     */
    public static class ServiceStats {
        private final int totalServices;
        private final int activeServices;
        private final int inactiveServices;
        private final int exceptionsToday;

        public ServiceStats(int totalServices, int activeServices, int inactiveServices, int exceptionsToday) {
            this.totalServices = totalServices;
            this.activeServices = activeServices;
            this.inactiveServices = inactiveServices;
            this.exceptionsToday = exceptionsToday;
        }

        public int getTotalServices() { return totalServices; }
        public int getActiveServices() { return activeServices; }
        public int getInactiveServices() { return inactiveServices; }
        public int getExceptionsToday() { return exceptionsToday; }

        @Override
        public String toString() {
            return String.format("ServiceStats{total=%d, active=%d, inactive=%d, exceptions=%d}",
                totalServices, activeServices, inactiveServices, exceptionsToday);
        }
    }
}