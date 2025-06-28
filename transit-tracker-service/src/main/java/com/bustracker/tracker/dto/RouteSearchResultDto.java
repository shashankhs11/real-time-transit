package com.bustracker.tracker.dto;

/**
 * DTO for route search results
 * Returns both display information and the actual routeId for API calls
 */
public class RouteSearchResultDto {
    
    private final String routeId;           // Internal ID for API calls (e.g., "6636")
    private final String routeShortName;    // Display name (e.g., "49")
    private final String routeLongName;     // Full name (e.g., "UBC - Metrotown Station")
    private final double relevanceScore;    // Search relevance score for ranking
    
    public RouteSearchResultDto(String routeId, String routeShortName, String routeLongName, double relevanceScore) {
        this.routeId = routeId;
        this.routeShortName = routeShortName;
        this.routeLongName = routeLongName;
        this.relevanceScore = relevanceScore;
    }
    
    public String getRouteId() {
        return routeId;
    }
    
    public String getRouteShortName() {
        return routeShortName;
    }
    
    public String getRouteLongName() {
        return routeLongName;
    }
    
    public double getRelevanceScore() {
        return relevanceScore;
    }
    
    @Override
    public String toString() {
        return String.format("RouteSearchResult{id='%s', shortName='%s', longName='%s', score=%.2f}",
            routeId, routeShortName, routeLongName, relevanceScore);
    }
}