package com.bustracker.tracker.service;

import com.bustracker.shared.model.VehiclePosition;
import com.bustracker.tracker.domain.ShapePoint;
import com.bustracker.tracker.domain.Stop;
import com.bustracker.tracker.domain.Trip;
import com.bustracker.tracker.repository.GtfsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShapeDistanceCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(ShapeDistanceCalculationService.class);
    
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double MAX_PROJECTION_DISTANCE_M = 500.0; // Max distance to consider vehicle on route

    private final GtfsRepository gtfsRepository;

    @Autowired
    public ShapeDistanceCalculationService(GtfsRepository gtfsRepository) {
        this.gtfsRepository = gtfsRepository;
    }

    /**
     * Calculate route-aware distance between vehicle and stop using shape data
     * @param vehiclePosition Current vehicle position  
     * @param trip Trip containing shape information
     * @param targetStop Target stop
     * @return ShapeDistanceResult with route distance and progress info
     */
    public ShapeDistanceResult calculateShapeDistance(VehiclePosition vehiclePosition, Trip trip, Stop targetStop) {
        logger.debug("Calculating shape distance for vehicle {} on trip {} to stop {}", 
            vehiclePosition.getVehicleId(), trip.getTripId(), targetStop.getStopId());

        if (!trip.hasShape()) {
            logger.debug("Trip {} has no shape data, falling back to straight-line", trip.getTripId());
            return null; // Caller should use fallback
        }

        List<ShapePoint> shapePoints = gtfsRepository.findShapePointsByShapeId(trip.getShapeId());
        if (shapePoints.isEmpty()) {
            logger.warn("No shape points found for shape_id: {}", trip.getShapeId());
            return null;
        }

        // Project vehicle position onto shape polyline
        ProjectionResult vehicleProjection = projectPointOntoShape(
            vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), shapePoints);
        
        if (!vehicleProjection.isOnRoute()) {
            logger.debug("Vehicle {} is too far from route shape ({}m), using fallback", 
                vehiclePosition.getVehicleId(), vehicleProjection.getDistanceFromRoute());
            return null;
        }

        // Project target stop onto shape polyline
        ProjectionResult stopProjection = projectPointOntoShape(
            targetStop.getStopLat(), targetStop.getStopLon(), shapePoints);

        // Calculate distance along route
        double routeDistance = calculateDistanceAlongShape(
            vehicleProjection.getShapeDistanceM(), stopProjection.getShapeDistanceM());

        // Calculate total route length for progress percentage
        double totalRouteLength = calculateTotalShapeDistance(shapePoints);
        double progressPercentage = (vehicleProjection.getShapeDistanceM() / totalRouteLength) * 100.0;

        logger.debug("Shape distance calculated: vehicle at {}m, stop at {}m, remaining: {}m, progress: {}%",
            vehicleProjection.getShapeDistanceM(), stopProjection.getShapeDistanceM(), 
            routeDistance, String.format("%.1f", progressPercentage));

        return new ShapeDistanceResult(
            routeDistance,
            progressPercentage,
            vehicleProjection.getShapeDistanceM(),
            stopProjection.getShapeDistanceM(),
            totalRouteLength
        );
    }

    /**
     * Project a geographic point onto the shape polyline
     */
    private ProjectionResult projectPointOntoShape(double lat, double lon, List<ShapePoint> shapePoints) {
        double minDistanceToRoute = Double.MAX_VALUE;
        double bestShapeDistance = 0.0;
        int bestSegmentIndex = -1;

        // Track cumulative distance along shape
        double cumulativeDistance = 0.0;
        
        for (int i = 0; i < shapePoints.size() - 1; i++) {
            ShapePoint p1 = shapePoints.get(i);
            ShapePoint p2 = shapePoints.get(i + 1);
            
            // Calculate distance between consecutive shape points
            double segmentLength = calculateDistance(
                p1.getShapePtLat(), p1.getShapePtLon(),
                p2.getShapePtLat(), p2.getShapePtLon()
            );

            // Project point onto this line segment
            SegmentProjection projection = projectPointOntoSegment(lat, lon, p1, p2);
            double distanceToSegment = projection.getDistanceToSegment();

            if (distanceToSegment < minDistanceToRoute) {
                minDistanceToRoute = distanceToSegment;
                bestShapeDistance = cumulativeDistance + (projection.getSegmentRatio() * segmentLength);
                bestSegmentIndex = i;
            }

            cumulativeDistance += segmentLength;
        }

        boolean isOnRoute = minDistanceToRoute <= MAX_PROJECTION_DISTANCE_M;
        
        logger.debug("Point projection: lat={:.6f}, lon={:.6f}, minDistance={:.1f}m, shapeDistance={:.1f}m, onRoute={}", 
            lat, lon, minDistanceToRoute, bestShapeDistance, isOnRoute);

        return new ProjectionResult(isOnRoute, minDistanceToRoute, bestShapeDistance, bestSegmentIndex);
    }

    /**
     * Project a point onto a line segment between two shape points
     */
    private SegmentProjection projectPointOntoSegment(double pointLat, double pointLon, 
                                                     ShapePoint p1, ShapePoint p2) {
        // Convert to projected coordinates for more accurate calculations
        double x1 = p1.getShapePtLon();
        double y1 = p1.getShapePtLat();
        double x2 = p2.getShapePtLon();
        double y2 = p2.getShapePtLat();
        double px = pointLon;
        double py = pointLat;

        double dx = x2 - x1;
        double dy = y2 - y1;
        
        if (dx == 0 && dy == 0) {
            // Degenerate segment - just return distance to point
            double distance = calculateDistance(pointLat, pointLon, p1.getShapePtLat(), p1.getShapePtLon());
            return new SegmentProjection(0.0, distance);
        }

        // Calculate parameter t for the projection point
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        
        // Clamp t to [0, 1] to stay within the segment
        t = Math.max(0.0, Math.min(1.0, t));
        
        // Calculate the projection point
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        
        // Calculate distance from original point to projection
        double distanceToSegment = calculateDistance(pointLat, pointLon, projY, projX);
        
        return new SegmentProjection(t, distanceToSegment);
    }

    /**
     * Calculate distance along the route between two positions on the shape
     */
    private double calculateDistanceAlongShape(double vehicleShapeDistance, double stopShapeDistance) {
        if (stopShapeDistance >= vehicleShapeDistance) {
            return stopShapeDistance - vehicleShapeDistance;
        } else {
            // Vehicle has passed the stop
            logger.debug("Vehicle has passed the stop: vehicle at {}m, stop at {}m", 
                vehicleShapeDistance, stopShapeDistance);
            return 0.0;
        }
    }

    /**
     * Calculate total distance along the entire shape
     */
    private double calculateTotalShapeDistance(List<ShapePoint> shapePoints) {
        double totalDistance = 0.0;
        
        for (int i = 0; i < shapePoints.size() - 1; i++) {
            ShapePoint p1 = shapePoints.get(i);
            ShapePoint p2 = shapePoints.get(i + 1);
            
            totalDistance += calculateDistance(
                p1.getShapePtLat(), p1.getShapePtLon(),
                p2.getShapePtLat(), p2.getShapePtLon()
            );
        }
        
        return totalDistance;
    }

    /**
     * Calculate distance between two geographic points using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /**
     * Result class for shape-based distance calculations
     */
    public static class ShapeDistanceResult {
        private final double routeDistanceM;
        private final double progressPercentage;
        private final double vehicleShapeDistanceM;
        private final double stopShapeDistanceM;
        private final double totalRouteDistanceM;

        public ShapeDistanceResult(double routeDistanceM, double progressPercentage, 
                                 double vehicleShapeDistanceM, double stopShapeDistanceM, 
                                 double totalRouteDistanceM) {
            this.routeDistanceM = routeDistanceM;
            this.progressPercentage = progressPercentage;
            this.vehicleShapeDistanceM = vehicleShapeDistanceM;
            this.stopShapeDistanceM = stopShapeDistanceM;
            this.totalRouteDistanceM = totalRouteDistanceM;
        }

        public double getRouteDistanceM() { return routeDistanceM; }
        public double getProgressPercentage() { return progressPercentage; }
        public double getVehicleShapeDistanceM() { return vehicleShapeDistanceM; }
        public double getStopShapeDistanceM() { return stopShapeDistanceM; }
        public double getTotalRouteDistanceM() { return totalRouteDistanceM; }

        @Override
        public String toString() {
            return String.format("ShapeDistanceResult{routeDistance=%.0fm, progress=%.1f%%, vehicle=%.0fm, stop=%.0fm}",
                routeDistanceM, progressPercentage, vehicleShapeDistanceM, stopShapeDistanceM);
        }
    }

    /**
     * Result class for point projection onto shape
     */
    private static class ProjectionResult {
        private final boolean onRoute;
        private final double distanceFromRoute;
        private final double shapeDistanceM;
        private final int segmentIndex;

        public ProjectionResult(boolean onRoute, double distanceFromRoute, double shapeDistanceM, int segmentIndex) {
            this.onRoute = onRoute;
            this.distanceFromRoute = distanceFromRoute;
            this.shapeDistanceM = shapeDistanceM;
            this.segmentIndex = segmentIndex;
        }

        public boolean isOnRoute() { return onRoute; }
        public double getDistanceFromRoute() { return distanceFromRoute; }
        public double getShapeDistanceM() { return shapeDistanceM; }
        public int getSegmentIndex() { return segmentIndex; }
    }

    /**
     * Result class for segment projection
     */
    private static class SegmentProjection {
        private final double segmentRatio; // 0.0 to 1.0 along the segment
        private final double distanceToSegment;

        public SegmentProjection(double segmentRatio, double distanceToSegment) {
            this.segmentRatio = segmentRatio;
            this.distanceToSegment = distanceToSegment;
        }

        public double getSegmentRatio() { return segmentRatio; }
        public double getDistanceToSegment() { return distanceToSegment; }
    }
}