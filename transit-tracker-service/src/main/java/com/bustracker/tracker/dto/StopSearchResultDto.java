package com.bustracker.tracker.dto;

/**
 * DTO for stop search results
 * Returns both display information and the actual stopId for API calls
 */
public class StopSearchResultDto {
    
    private final String stopId;            // Internal ID for API calls (e.g., "12345")
    private final String stopName;          // Display name (e.g., "Westbound E 49 Ave @ Prince Edward St")
    private final double stopLat;           // Latitude for map display
    private final double stopLon;           // Longitude for map display
    private final double relevanceScore;    // Search relevance score for ranking
    
    public StopSearchResultDto(String stopId, String stopName, double stopLat, double stopLon, double relevanceScore) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.stopLat = stopLat;
        this.stopLon = stopLon;
        this.relevanceScore = relevanceScore;
    }
    
    public String getStopId() {
        return stopId;
    }
    
    public String getStopName() {
        return stopName;
    }
    
    public double getStopLat() {
        return stopLat;
    }
    
    public double getStopLon() {
        return stopLon;
    }
    
    public double getRelevanceScore() {
        return relevanceScore;
    }
    
    @Override
    public String toString() {
        return String.format("StopSearchResult{id='%s', name='%s', lat=%.6f, lon=%.6f, score=%.2f}",
            stopId, stopName, stopLat, stopLon, relevanceScore);
    }
}