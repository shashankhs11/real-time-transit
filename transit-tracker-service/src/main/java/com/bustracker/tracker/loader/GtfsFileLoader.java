package com.bustracker.tracker.loader;

import com.bustracker.tracker.domain.Route;
import com.bustracker.tracker.domain.Stop;
import com.bustracker.tracker.domain.Trip;
import com.bustracker.tracker.domain.StopTime;
import com.bustracker.tracker.domain.ShapePoint;
import com.bustracker.tracker.domain.DirectionName;
import com.bustracker.tracker.domain.Calendar;
import com.bustracker.tracker.domain.CalendarDate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GtfsFileLoader {

  private static final Logger logger = LoggerFactory.getLogger(GtfsFileLoader.class);

  /**
   * Load routes from GTFS routes.txt file
   */
  public List<Route> loadRoutes(InputStream routesInputStream) throws IOException {
    logger.info("📂 Loading routes from GTFS routes.txt");

    List<Route> routes = new ArrayList<>();

    try (Reader reader = new InputStreamReader(routesInputStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

      for (CSVRecord record : csvParser) {
        try {
          String routeId = record.get("route_id");
          String routeShortName = record.get("route_short_name");
          String routeLongName = record.get("route_long_name");
          int routeType = Integer.parseInt(record.get("route_type"));

          // Only load bus routes (route_type = 3)
          if (routeType == 3) {
            Route route = new Route(routeId, routeShortName, routeLongName, routeType);
            routes.add(route);
          }

        } catch (Exception e) {
          logger.warn("❌ Failed to parse route record: {}", record, e);
        }
      }
    }

    logger.info("✅ Loaded {} bus routes", routes.size());
    return routes;
  }

  /**
   * Load stops from GTFS stops.txt file
   */
  public List<Stop> loadStops(InputStream stopsInputStream) throws IOException {
    logger.info("📂 Loading stops from GTFS stops.txt");

    List<Stop> stops = new ArrayList<>();

    try (Reader reader = new InputStreamReader(stopsInputStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

      for (CSVRecord record : csvParser) {
        try {
          String stopId = record.get("stop_id");
          String stopName = record.get("stop_name");
          double stopLat = Double.parseDouble(record.get("stop_lat"));
          double stopLon = Double.parseDouble(record.get("stop_lon"));

          Stop stop = new Stop(stopId, stopName, stopLat, stopLon);
          stops.add(stop);

        } catch (Exception e) {
          logger.warn("❌ Failed to parse stop record: {}", record, e);
        }
      }
    }

    logger.info("✅ Loaded {} stops", stops.size());
    return stops;
  }

  /**
   * Load trips from GTFS trips.txt file
   */
  public List<Trip> loadTrips(InputStream tripsInputStream) throws IOException {
    logger.info("📂 Loading trips from GTFS trips.txt");

    List<Trip> trips = new ArrayList<>();

    try (Reader reader = new InputStreamReader(tripsInputStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

      for (CSVRecord record : csvParser) {
        try {
          String tripId = record.get("trip_id");
          String routeId = record.get("route_id");
          String serviceId = record.get("service_id");
          String shapeId = record.isSet("shape_id") ? record.get("shape_id") : null;
          int directionId = Integer.parseInt(record.get("direction_id"));
          String tripHeadsign = record.isSet("trip_headsign") ? record.get("trip_headsign") : null;

          Trip trip = new Trip(tripId, routeId, serviceId, shapeId, directionId, tripHeadsign);
          trips.add(trip);

        } catch (Exception e) {
          logger.warn("❌ Failed to parse trip record: {}", record, e);
        }
      }
    }

    logger.info("✅ Loaded {} trips", trips.size());
    return trips;
  }

  /**
   * Load stop times from GTFS stop_times.txt file
   * This is the CRITICAL file that connects trips to stops
   */
  public List<StopTime> loadStopTimes(InputStream stopTimesInputStream) throws IOException {
    logger.info("📂 Loading stop times from GTFS stop_times.txt");

    List<StopTime> stopTimes = new ArrayList<>();
    int skippedRecords = 0;

    try (Reader reader = new InputStreamReader(stopTimesInputStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

      for (CSVRecord record : csvParser) {
        try {
          String tripId = record.get("trip_id");
          String stopId = record.get("stop_id");
          String arrivalTimeStr = record.get("arrival_time");
          String departureTimeStr = record.get("departure_time");
          int stopSequence = Integer.parseInt(record.get("stop_sequence"));

          var arrivalTime = StopTime.parseGtfsTime(arrivalTimeStr);
          var departureTime = StopTime.parseGtfsTime(departureTimeStr);

          if (arrivalTime != null) {
            StopTime stopTime = new StopTime(tripId, stopId, arrivalTime, departureTime, stopSequence);
            stopTimes.add(stopTime);
          } else {
            skippedRecords++;
          }

        } catch (Exception e) {
          logger.warn("❌ Failed to parse stop time record: {}", record, e);
          skippedRecords++;
        }
      }
    }

    if (skippedRecords > 0) {
      logger.warn("⚠️ Skipped {} invalid stop time records", skippedRecords);
    }

    logger.info("✅ Loaded {} stop times", stopTimes.size());
    return stopTimes;
  }

  /**
   * Load direction names from GTFS direction_names_exceptions.txt file
   * TransLink-specific file with format: route_name,direction_id,direction_name,direction_do
   */
  public List<DirectionName> loadDirectionNames(InputStream directionNamesInputStream) throws IOException {
    logger.info("📂 Loading direction names from GTFS direction_names_exceptions.txt");

    List<DirectionName> directionNames = new ArrayList<>();

    try (Reader reader = new InputStreamReader(directionNamesInputStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

      // Log the headers to debug BOM issues
      var headers = csvParser.getHeaderNames();
      logger.debug("CSV Headers found: {}", headers);

      for (CSVRecord record : csvParser) {
        try {
          // Handle potential BOM character by trying multiple header variations
          String routeName = null;
          String directionName = null;
          String directionDo = null;
          int directionId = -1;

          // Try different header variations to handle BOM
          for (String header : headers) {
            String cleanHeader = header.trim().replaceAll("^\\uFEFF", ""); // Remove BOM

            if (cleanHeader.equals("route_name") || header.contains("route_name")) {
              routeName = record.get(header);
            } else if (cleanHeader.equals("direction_id") || header.contains("direction_id")) {
              directionId = Integer.parseInt(record.get(header));
            } else if (cleanHeader.equals("direction_name") || header.contains("direction_name")) {
              directionName = record.get(header);
            } else if (cleanHeader.equals("direction_do") || header.contains("direction_do")) {
              directionDo = record.get(header);
            }
          }

          if (routeName != null && directionName != null && directionId >= 0) {
            DirectionName direction = new DirectionName(routeName, directionId, directionName, directionDo);
            directionNames.add(direction);
          } else {
            logger.warn("❌ Incomplete direction name record: routeName={}, directionId={}, directionName={}",
                routeName, directionId, directionName);
          }

        } catch (Exception e) {
          logger.warn("❌ Failed to parse direction name record: {}", record, e);
        }
      }
    }

    logger.info("✅ Loaded {} direction names", directionNames.size());
    return directionNames;
  }
  public List<ShapePoint> loadShapePoints(InputStream shapesInputStream) throws IOException {
    logger.info("📂 Loading shape points from GTFS shapes.txt");

    List<ShapePoint> shapePoints = new ArrayList<>();

    try (Reader reader = new InputStreamReader(shapesInputStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

      for (CSVRecord record : csvParser) {
        try {
          String shapeId = record.get("shape_id");
          double shapePtLat = Double.parseDouble(record.get("shape_pt_lat"));
          double shapePtLon = Double.parseDouble(record.get("shape_pt_lon"));
          int shapePtSequence = Integer.parseInt(record.get("shape_pt_sequence"));

          ShapePoint shapePoint = new ShapePoint(shapeId, shapePtLat, shapePtLon, shapePtSequence);
          shapePoints.add(shapePoint);

        } catch (Exception e) {
          logger.warn("❌ Failed to parse shape point record: {}", record, e);
        }
      }
    }

    logger.info("✅ Loaded {} shape points", shapePoints.size());
    return shapePoints;
  }

  /**
   * Extract a specific file from the GTFS zip and return as InputStream
   */
  public InputStream extractFileFromZip(String zipFilePath, String fileName) throws IOException {
    logger.debug("📂 Extracting {} from {}", fileName, zipFilePath);

    try (FileInputStream fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.getName().equals(fileName)) {
          // Read the entire file into a ByteArrayInputStream
          // This allows us to return an InputStream that can be used after the ZIP is closed
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int len;
          while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
          }
          return new ByteArrayInputStream(baos.toByteArray());
        }
      }
    }

    throw new FileNotFoundException("File " + fileName + " not found in ZIP: " + zipFilePath);
  }

  // Convenience methods for loading from zip file

  public List<Route> loadRoutesFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "routes.txt")) {
      return loadRoutes(inputStream);
    }
  }

  public List<Stop> loadStopsFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "stops.txt")) {
      return loadStops(inputStream);
    }
  }

  public List<Trip> loadTripsFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "trips.txt")) {
      return loadTrips(inputStream);
    }
  }

  public List<StopTime> loadStopTimesFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "stop_times.txt")) {
      return loadStopTimes(inputStream);
    }
  }

  public List<ShapePoint> loadShapePointsFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "shapes.txt")) {
      return loadShapePoints(inputStream);
    }
  }

  public List<DirectionName> loadDirectionNamesFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "direction_names_exceptions.txt")) {
      return loadDirectionNames(inputStream);
    }
  }

  /**
   * Load calendars from GTFS calendar.txt file
   */
  public List<Calendar> loadCalendars(InputStream inputStream) throws IOException {
    List<Calendar> calendars = new ArrayList<>();
    
    try (CSVParser parser = CSVFormat.DEFAULT
        .withFirstRecordAsHeader()
        .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      
      for (CSVRecord record : parser) {
        try {
          String serviceId = record.get("service_id");
          String startDateStr = record.get("start_date");
          String endDateStr = record.get("end_date");
          String mondayStr = record.get("monday");
          String tuesdayStr = record.get("tuesday");
          String wednesdayStr = record.get("wednesday");
          String thursdayStr = record.get("thursday");
          String fridayStr = record.get("friday");
          String saturdayStr = record.get("saturday");
          String sundayStr = record.get("sunday");
          
          Calendar calendar = new Calendar(
              serviceId,
              Calendar.parseGtfsDate(startDateStr),
              Calendar.parseGtfsDate(endDateStr),
              Calendar.parseGtfsBoolean(mondayStr),
              Calendar.parseGtfsBoolean(tuesdayStr),
              Calendar.parseGtfsBoolean(wednesdayStr),
              Calendar.parseGtfsBoolean(thursdayStr),
              Calendar.parseGtfsBoolean(fridayStr),
              Calendar.parseGtfsBoolean(saturdayStr),
              Calendar.parseGtfsBoolean(sundayStr)
          );
          
          calendars.add(calendar);
          
        } catch (Exception e) {
          logger.warn("Error parsing calendar record: {}", record, e);
        }
      }
    }
    
    logger.info("Loaded {} calendars", calendars.size());
    return calendars;
  }

  /**
   * Load calendar dates from GTFS calendar_dates.txt file
   */
  public List<CalendarDate> loadCalendarDates(InputStream inputStream) throws IOException {
    List<CalendarDate> calendarDates = new ArrayList<>();
    
    try (CSVParser parser = CSVFormat.DEFAULT
        .withFirstRecordAsHeader()
        .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      
      for (CSVRecord record : parser) {
        try {
          String serviceId = record.get("service_id");
          String dateStr = record.get("date");
          String exceptionTypeStr = record.get("exception_type");
          
          CalendarDate calendarDate = new CalendarDate(
              serviceId,
              CalendarDate.parseGtfsDate(dateStr),
              CalendarDate.ExceptionType.fromString(exceptionTypeStr)
          );
          
          calendarDates.add(calendarDate);
          
        } catch (Exception e) {
          logger.warn("Error parsing calendar date record: {}", record, e);
        }
      }
    }
    
    logger.info("Loaded {} calendar dates", calendarDates.size());
    return calendarDates;
  }

  public List<Calendar> loadCalendarsFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "calendar.txt")) {
      return loadCalendars(inputStream);
    }
  }

  public List<CalendarDate> loadCalendarDatesFromZip(String zipFilePath) throws IOException {
    try (InputStream inputStream = extractFileFromZip(zipFilePath, "calendar_dates.txt")) {
      return loadCalendarDates(inputStream);
    }
  }
}