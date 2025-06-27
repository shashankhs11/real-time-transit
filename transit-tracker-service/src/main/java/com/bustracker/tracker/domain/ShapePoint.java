package com.bustracker.tracker.domain;

import java.util.Objects;

/**
 * Domain model representing a shape point from GTFS shapes.txt
 * Forms ordered polylines for trip paths
 */
public class ShapePoint {

  private final String shapeId;
  private final double shapePtLat;
  private final double shapePtLon;
  private final int shapePtSequence;

  public ShapePoint(String shapeId, double shapePtLat, double shapePtLon, int shapePtSequence) {
    this.shapeId = Objects.requireNonNull(shapeId, "Shape ID cannot be null");
    this.shapePtLat = shapePtLat;
    this.shapePtLon = shapePtLon;
    this.shapePtSequence = shapePtSequence;

    // Basic validation for Vancouver area coordinates
    if (shapePtLat < 49.0 || shapePtLat > 49.5) {
      throw new IllegalArgumentException("Invalid latitude for Vancouver area: " + shapePtLat);
    }
    if (shapePtLon < -123.5 || shapePtLon > -122.0) {
      throw new IllegalArgumentException("Invalid longitude for Vancouver area: " + shapePtLon);
    }

    if (shapePtSequence < 0) {
      throw new IllegalArgumentException("Shape point sequence must be non-negative: " + shapePtSequence);
    }
  }

  public String getShapeId() {
    return shapeId;
  }

  public double getShapePtLat() {
    return shapePtLat;
  }

  public double getShapePtLon() {
    return shapePtLon;
  }

  public int getShapePtSequence() {
    return shapePtSequence;
  }

  /**
   * Calculate distance to another point using Haversine formula
   */
  public double distanceToPoint(double lat, double lon) {
    final double R = 6371000; // Earth's radius in meters
    double dLat = Math.toRadians(lat - this.shapePtLat);
    double dLon = Math.toRadians(lon - this.shapePtLon);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(this.shapePtLat)) * Math.cos(Math.toRadians(lat)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ShapePoint that = (ShapePoint) o;
    return Objects.equals(shapeId, that.shapeId) &&
        shapePtSequence == that.shapePtSequence;
  }

  @Override
  public int hashCode() {
    return Objects.hash(shapeId, shapePtSequence);
  }

  @Override
  public String toString() {
    return String.format("ShapePoint{shapeId='%s', seq=%d, lat=%.6f, lon=%.6f}",
        shapeId, shapePtSequence, shapePtLat, shapePtLon);
  }
}