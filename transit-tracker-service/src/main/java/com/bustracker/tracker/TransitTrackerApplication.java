package com.bustracker.tracker;

import com.bustracker.tracker.domain.DirectionName;
import com.bustracker.tracker.domain.Stop;
import com.bustracker.tracker.loader.GtfsFileLoader;
import com.bustracker.tracker.repository.GtfsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TransitTrackerApplication implements CommandLineRunner {

  @Autowired
  private GtfsFileLoader gtfsFileLoader;

  @Autowired
  private GtfsRepository gtfsRepository;

  public static void main(String[] args) {
    SpringApplication.run(TransitTrackerApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    System.out.println("🚀 Transit Tracker Service Starting...");

    // Test complete GTFS loading (update this path)
    String zipPath = "google_transit.zip";

    try {
      System.out.println("🔍 Loading complete GTFS dataset...");

      // Load all GTFS files
      System.out.println("📂 Loading routes...");
      var routes = gtfsFileLoader.loadRoutesFromZip(zipPath);
      gtfsRepository.loadRoutes(routes);

      System.out.println("📂 Loading stops...");
      var stops = gtfsFileLoader.loadStopsFromZip(zipPath);
      gtfsRepository.loadStops(stops);

      System.out.println("📂 Loading trips...");
      var trips = gtfsFileLoader.loadTripsFromZip(zipPath);
      gtfsRepository.loadTrips(trips);

      System.out.println("📂 Loading stop times (this may take a moment)...");
      var stopTimes = gtfsFileLoader.loadStopTimesFromZip(zipPath);
      gtfsRepository.loadStopTimes(stopTimes);

      System.out.println("📂 Loading shape points (this may take a moment)...");
      var shapePoints = gtfsFileLoader.loadShapePointsFromZip(zipPath);
      gtfsRepository.loadShapePoints(shapePoints);

      System.out.println("📂 Loading direction names...");
      var directionNames = gtfsFileLoader.loadDirectionNamesFromZip(zipPath);
      gtfsRepository.loadDirectionNames(directionNames);

      System.out.println("📂 Loading calendars...");
      var calendars = gtfsFileLoader.loadCalendarsFromZip(zipPath);
      gtfsRepository.loadCalendars(calendars);

      System.out.println("📂 Loading calendar dates...");
      var calendarDates = gtfsFileLoader.loadCalendarDatesFromZip(zipPath);
      gtfsRepository.loadCalendarDates(calendarDates);

      // Display repository statistics
      System.out.println("📊 Complete GTFS Repository Stats: " + gtfsRepository.getStats());

      // Test the complete user flow for route 49
      System.out.println("\n🧪 Testing User Flow for Route 49:");

      // Step 1: Find route by short name
      var route49List = gtfsRepository.findRoutesByShortName("049");
      if (route49List.isEmpty()) {
        System.out.println("❌ No route found for '49'");
        return;
      }

      var route49 = route49List.get(0);
      System.out.println("✅ Step 1 - Found route: " + route49);

      // Step 2: Find directions for this route with friendly names
      var tripsDir0 = gtfsRepository.findTripsByRouteAndDirection(route49.getRouteId(), 0);
      var tripsDir1 = gtfsRepository.findTripsByRouteAndDirection(route49.getRouteId(), 1);

      // Test direction name lookup
      var dir0Name = gtfsRepository.findDirectionNameByRouteAndDirection("49", 0);
      var dir1Name = gtfsRepository.findDirectionNameByRouteAndDirection("49", 1);

      System.out.println("✅ Step 2 - Directions found:");
      System.out.println("   Direction 0: " + tripsDir0.size() + " trips - " +
          dir0Name.map(DirectionName::getDirectionName).orElse("No friendly name"));
      System.out.println("   Direction 1: " + tripsDir1.size() + " trips - " +
          dir1Name.map(DirectionName::getDirectionName).orElse("No friendly name"));

      // Step 3: Get stops for direction 0
      var stopsDir0 = gtfsRepository.findStopsForRouteAndDirection(route49.getRouteId(), 0);
      System.out.println("✅ Step 3 - Stops for Direction 0: " + stopsDir0.size() + " stops");

      // Show first few stops
      System.out.println("   First 5 stops:");
      stopsDir0.stream().limit(5).forEach(stop ->
          System.out.println("      " + stop.getStopName() + " (ID: " + stop.getStopId() + ")"));

      // Step 4: Test stop times for representative trip
      var repTrip = gtfsRepository.findRepresentativeTripForRouteAndDirection(route49.getRouteId(), 0);
      if (repTrip.isPresent()) {
        var stopTimesForTrip = gtfsRepository.findStopTimesByTripId(repTrip.get().getTripId());
        System.out.println("✅ Step 4 - Stop times for representative trip: " + stopTimesForTrip.size() + " scheduled stops");

        // Show first few stop times
        System.out.println("   First 3 scheduled times:");
        stopTimesForTrip.stream().limit(3).forEach(stopTime -> {
          var stop = gtfsRepository.findStopById(stopTime.getStopId());
          String stopName = stop.map(Stop::getStopName).orElse("Unknown Stop");
          System.out.println("      " + stopTime.getArrivalTime() + " at " + stopName);
        });
      }

      // Test shape data if available
      if (repTrip.isPresent() && repTrip.get().hasShape()) {
        var shapePointsForTrip = gtfsRepository.findShapePointsByShapeId(repTrip.get().getShapeId());
        System.out.println("✅ Shape data - Route path has " + shapePointsForTrip.size() + " coordinate points");
      }

      System.out.println("\n🎉 Complete GTFS loading and user flow test successful!");
      System.out.println("🎯 Ready for REST API endpoints and real-time integration");

    } catch (Exception e) {
      System.out.println("❌ GTFS loading failed: " + e.getMessage());
      System.out.println("💡 Make sure to update the zipPath variable with your actual file path");
    }
  }
}