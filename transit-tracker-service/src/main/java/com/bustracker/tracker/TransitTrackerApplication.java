package com.bustracker.tracker;

import com.bustracker.tracker.loader.GtfsFileLoader;
import com.bustracker.tracker.repository.GtfsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TransitTrackerApplication implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(TransitTrackerApplication.class);

  @Autowired
  private GtfsFileLoader gtfsFileLoader;

  @Autowired
  private GtfsRepository gtfsRepository;

  public static void main(String[] args) {
    SpringApplication.run(TransitTrackerApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    loadGtfsData();
  }
  
  private void loadGtfsData() {
    logger.info("Starting GTFS data loading process");
    
    String zipPath = System.getenv("GTFS_ZIP_PATH");
    if (zipPath == null || zipPath.trim().isEmpty()) {
      zipPath = "google_transit.zip";
      logger.debug("GTFS_ZIP_PATH environment variable not set, using default: {}", zipPath);
    } else {
      logger.debug("Using GTFS_ZIP_PATH from environment: {}", zipPath);
    }
    
    try {
      logger.info("Loading GTFS dataset from: {}", zipPath);
      
      // Load all GTFS files
      logger.debug("Loading routes from GTFS");
      var routes = gtfsFileLoader.loadRoutesFromZip(zipPath);
      gtfsRepository.loadRoutes(routes);
      
      logger.debug("Loading stops from GTFS");
      var stops = gtfsFileLoader.loadStopsFromZip(zipPath);
      gtfsRepository.loadStops(stops);
      
      logger.debug("Loading trips from GTFS");
      var trips = gtfsFileLoader.loadTripsFromZip(zipPath);
      gtfsRepository.loadTrips(trips);
      
      logger.debug("Loading stop times from GTFS");
      var stopTimes = gtfsFileLoader.loadStopTimesFromZip(zipPath);
      gtfsRepository.loadStopTimes(stopTimes);
      
      logger.debug("Loading shape points from GTFS");
      var shapePoints = gtfsFileLoader.loadShapePointsFromZip(zipPath);
      gtfsRepository.loadShapePoints(shapePoints);
      
      logger.debug("Loading direction names from GTFS");
      var directionNames = gtfsFileLoader.loadDirectionNamesFromZip(zipPath);
      gtfsRepository.loadDirectionNames(directionNames);
      
      logger.debug("Loading calendars from GTFS");
      var calendars = gtfsFileLoader.loadCalendarsFromZip(zipPath);
      gtfsRepository.loadCalendars(calendars);
      
      logger.debug("Loading calendar dates from GTFS");
      var calendarDates = gtfsFileLoader.loadCalendarDatesFromZip(zipPath);
      gtfsRepository.loadCalendarDates(calendarDates);
      
      // Log final statistics
      var stats = gtfsRepository.getStats();
      logger.info("GTFS data loading completed successfully. Repository stats: {}", stats);
      
      // Perform basic validation
      validateGtfsData();
      
    } catch (Exception e) {
      logger.error("Failed to load GTFS data from: {}. Error: {}", zipPath, e.getMessage(), e);
      throw new RuntimeException("GTFS data loading failed", e);
    }
  }
  
  private void validateGtfsData() {
    logger.debug("Performing GTFS data validation");
    
    try {
      // Basic validation - check if we have essential data
      var allRoutes = gtfsRepository.findAllRoutes();
      var allStops = gtfsRepository.findAllStops();
      var allTrips = gtfsRepository.findAllTrips();
      
      if (allRoutes.isEmpty() || allStops.isEmpty() || allTrips.isEmpty()) {
        throw new IllegalStateException("Essential GTFS data is missing");
      }
      
      logger.info("GTFS data validation passed. Loaded {} routes, {} stops, {} trips", 
                  allRoutes.size(), allStops.size(), allTrips.size());
                  
    } catch (Exception e) {
      logger.error("GTFS data validation failed", e);
      throw new RuntimeException("GTFS data validation failed", e);
    }
  }
}