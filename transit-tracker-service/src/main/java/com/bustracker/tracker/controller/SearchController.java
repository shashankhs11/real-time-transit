package com.bustracker.tracker.controller;

import com.bustracker.tracker.dto.RouteSearchResultDto;
import com.bustracker.tracker.dto.StopSearchResultDto;
import com.bustracker.tracker.domain.Route;
import com.bustracker.tracker.domain.Stop;
import com.bustracker.tracker.repository.GtfsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for search endpoints
 * Provides efficient route and stop search functionality with relevance scoring
 * 
 * Search Features:
 * 1. Route Search (/api/search/routes?q={query}):
 *    - Searches by route short name (e.g., "49", "R4", "99")
 *    - Returns routeId for API calls (e.g., "6636" for route "49")
 *    - Fuzzy matching with relevance scoring
 *    - Exact matches ranked highest, partial matches ranked lower
 * 
 * 2. Stop Search (/api/search/stops?q={query}):
 *    - Searches by stop name substring (e.g., "Princ" finds "Prince Edward St")
 *    - Case-insensitive partial matching
 *    - Word boundary matching gets higher relevance scores
 *    - Returns coordinates for map display
 * 
 * Both endpoints support:
 * - Configurable result limits
 * - Relevance-based sorting
 * - Efficient repository-level filtering
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private static final int MAX_SEARCH_RESULTS = 20;
    private static final int MIN_QUERY_LENGTH = 1;

    private final GtfsRepository gtfsRepository;

    @Autowired
    public SearchController(GtfsRepository gtfsRepository) {
        this.gtfsRepository = gtfsRepository;
    }

    /**
     * GET /api/search/routes?q={query}
     * Search for routes by short name with fuzzy matching
     * 
     * Examples:
     * - /api/search/routes?q=49 → Returns route 49 with routeId "6636"
     * - /api/search/routes?q=R4 → Returns R4 routes
     * - /api/search/routes?q=9 → Returns routes 9, 99, 209, etc.
     */
    @GetMapping("/routes")
    public ResponseEntity<List<RouteSearchResultDto>> searchRoutes(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        logger.info("🔍 Searching routes for query: '{}'", query);
        
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            logger.warn("❌ Query too short: '{}'", query);
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String normalizedQuery = query.trim().toUpperCase();
            List<Route> matchingRoutes = gtfsRepository.searchRoutesByShortName(query);
            
            List<RouteSearchResultDto> searchResults = matchingRoutes.stream()
                .map(route -> calculateRouteRelevance(route, normalizedQuery))
                .filter(result -> result.getRelevanceScore() > 0) // Only include matches
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore())) // Sort by relevance
                .limit(Math.min(limit, MAX_SEARCH_RESULTS))
                .collect(Collectors.toList());
            
            logger.info("✅ Found {} route matches for query '{}'", searchResults.size(), query);
            return ResponseEntity.ok(searchResults);
            
        } catch (Exception e) {
            logger.error("❌ Error searching routes for query '{}'", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/search/stops?q={query}
     * Search for stops by name with substring matching
     * 
     * Examples:
     * - /api/search/stops?q=Princ → Returns "Westbound E 49 Ave @ Prince Edward St"
     * - /api/search/stops?q=Metrotown → Returns "Metrotown Station @ Bay 11", etc.
     * - /api/search/stops?q=Fraser → Returns all stops with "Fraser" in the name
     */
    @GetMapping("/stops")
    public ResponseEntity<List<StopSearchResultDto>> searchStops(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "15") int limit) {
        
        logger.info("🔍 Searching stops for query: '{}'", query);
        
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            logger.warn("❌ Query too short: '{}'", query);
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String normalizedQuery = query.trim().toLowerCase();
            List<Stop> matchingStops = gtfsRepository.searchStopsByName(query);
            
            List<StopSearchResultDto> searchResults = matchingStops.stream()
                .map(stop -> calculateStopRelevance(stop, normalizedQuery))
                .filter(result -> result.getRelevanceScore() > 0) // Only include matches
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore())) // Sort by relevance
                .limit(Math.min(limit, MAX_SEARCH_RESULTS))
                .collect(Collectors.toList());
            
            logger.info("✅ Found {} stop matches for query '{}'", searchResults.size(), query);
            return ResponseEntity.ok(searchResults);
            
        } catch (Exception e) {
            logger.error("❌ Error searching stops for query '{}'", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Calculate relevance score for route search
     * Higher scores indicate better matches
     */
    private RouteSearchResultDto calculateRouteRelevance(Route route, String normalizedQuery) {
        String routeShortName = route.getRouteShortName().toUpperCase();
        String routeLongName = route.getRouteLongName().toUpperCase();
        
        double score = 0.0;
        
        // Exact match on short name gets highest score
        if (routeShortName.equals(normalizedQuery)) {
            score = 100.0;
        }
        // Starts with query gets high score
        else if (routeShortName.startsWith(normalizedQuery)) {
            score = 80.0;
        }
        // Contains query in short name gets medium score
        else if (routeShortName.contains(normalizedQuery)) {
            score = 60.0;
        }
        // Contains query in long name gets lower score
        else if (routeLongName.contains(normalizedQuery)) {
            score = 40.0;
        }
        
        // Boost score for shorter route names (more specific matches)
        if (score > 0 && routeShortName.length() <= 3) {
            score += 10.0;
        }
        
        return new RouteSearchResultDto(
            route.getRouteId(),
            route.getRouteShortName(),
            route.getRouteLongName(),
            score
        );
    }

    /**
     * Calculate relevance score for stop search
     * Higher scores indicate better matches
     */
    private StopSearchResultDto calculateStopRelevance(Stop stop, String normalizedQuery) {
        String stopName = stop.getStopName().toLowerCase();
        
        double score = 0.0;
        
        // Find the position of the query in the stop name
        int queryIndex = stopName.indexOf(normalizedQuery);
        
        if (queryIndex == -1) {
            // No match
            score = 0.0;
        } else {
            // Base score for containing the query
            score = 50.0;
            
            // Boost score based on position (earlier = better)
            if (queryIndex == 0) {
                // Starts with query gets highest boost
                score += 50.0;
            } else if (queryIndex <= 10) {
                // Near the beginning gets medium boost
                score += 30.0;
            } else {
                // Later in the string gets small boost
                score += 10.0;
            }
            
            // Boost score for word boundary matches
            if (queryIndex == 0 || stopName.charAt(queryIndex - 1) == ' ') {
                score += 20.0;
            }
            
            // Boost score based on query length relative to stop name
            double lengthRatio = (double) normalizedQuery.length() / stopName.length();
            score += lengthRatio * 20.0;
        }
        
        return new StopSearchResultDto(
            stop.getStopId(),
            stop.getStopName(),
            stop.getStopLat(),
            stop.getStopLon(),
            score
        );
    }
}