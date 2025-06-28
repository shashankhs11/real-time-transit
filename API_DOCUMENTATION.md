# Real-Time Transit Tracker API Documentation

This document provides comprehensive API documentation for the Real-Time Transit Tracker system. The API enables frontend applications to search for routes and stops, get route information, and retrieve real-time bus arrival data.

## Base URL
```
http://localhost:8082/api
```

## API Endpoints Overview

### Search Endpoints
- [Search Routes](#search-routes) - Find routes by name/number
- [Search Stops](#search-stops) - Find stops within a specific route

### Route Information Endpoints  
- [Get All Routes](#get-all-routes) - List all available routes
- [Get Route Directions](#get-route-directions) - Get directions for a route
- [Get Route Stops](#get-route-stops) - Get stops for a route and direction

### Real-Time Data Endpoints
- [Get Stop Arrivals](#get-stop-arrivals) - Real-time and scheduled arrivals
- [Get Route Vehicle Stats](#get-route-vehicle-stats) - Live vehicle statistics

---

## Search Endpoints

### Search Routes

Search for transit routes by route number or name with intelligent matching.

**URL:** `/search/routes`  
**Method:** `GET`  
**Query Parameters:**
- `q` (required): Search query (route number/name)
- `limit` (optional): Maximum results to return (default: 10, max: 20)

**Features:**
- Handles padded route numbers: "2" matches "002", "49" matches "049"
- Fuzzy matching with relevance scoring
- Returns actual `routeId` needed for other API calls

**Example Request:**
```http
GET /api/search/routes?q=49&limit=5
```

**Sample Response:**
```json
[
  {
    "routeId": "6636",
    "routeShortName": "049",
    "routeLongName": "UBC - Metrotown Station",
    "relevanceScore": 100.0
  },
  {
    "routeId": "6637", 
    "routeShortName": "049",
    "routeLongName": "Metrotown Station - UBC",
    "relevanceScore": 100.0
  }
]
```

**More Examples:**
```http
GET /api/search/routes?q=2        # Finds route "002"
GET /api/search/routes?q=R4       # Finds R4 routes  
GET /api/search/routes?q=Metro    # Finds routes with "Metro" in name
```

---

### Search Stops

Search for stops within a specific route and direction.

**URL:** `/search/stops`  
**Method:** `GET`  
**Query Parameters:**
- `q` (required): Search query (stop name substring)
- `routeId` (required): Route ID from route search
- `directionId` (required): Direction ID (0 or 1)
- `limit` (optional): Maximum results to return (default: 15, max: 20)

**Features:**
- Only searches stops served by the specified route and direction
- Case-insensitive substring matching
- Word boundary matching gets higher relevance scores

**Example Request:**
```http
GET /api/search/stops?q=Princ&routeId=6636&directionId=0&limit=10
```

**Sample Response:**
```json
[
  {
    "stopId": "1989",
    "stopName": "Westbound E 49 Ave @ Prince Edward St",
    "stopLat": 49.2301,
    "stopLon": -123.0987,
    "relevanceScore": 90.0
  },
  {
    "stopId": "1988", 
    "stopName": "Westbound E 49 Ave @ Prince Albert St",
    "stopLat": 49.2299,
    "stopLon": -123.0985,
    "relevanceScore": 75.0
  }
]
```

**More Examples:**
```http
GET /api/search/stops?q=Metro&routeId=6636&directionId=1    # Metrotown stops on route 49 dir 1
GET /api/search/stops?q=Fraser&routeId=6636&directionId=0   # Fraser St stops on route 49 dir 0
GET /api/search/stops?q=Bay&routeId=6636&directionId=1      # Bay stops on route 49 dir 1
```

---

## Route Information Endpoints

### Get All Routes

Retrieve all available transit routes.

**URL:** `/routes`  
**Method:** `GET`  
**Query Parameters:** None

**Example Request:**
```http
GET /api/routes
```

**Sample Response:**
```json
[
  {
    "routeId": "6636",
    "routeShortName": "049", 
    "routeLongName": "UBC - Metrotown Station"
  },
  {
    "routeId": "6637",
    "routeShortName": "002",
    "routeLongName": "Downtown - Brentwood"
  },
  {
    "routeId": "6638",
    "routeShortName": "099",
    "routeLongName": "Commercial Broadway - UBC"
  }
]
```

---

### Get Route Directions

Get available directions for a specific route with friendly names.

**URL:** `/routes/{routeId}/directions`  
**Method:** `GET`  
**Path Parameters:**
- `routeId`: The route ID (from route search or route list)

**Example Request:**
```http
GET /api/routes/6636/directions
```

**Sample Response:**
```json
[
  {
    "directionId": 0,
    "directionName": "Direction 0",
    "tripHeadsign": "49 Metrotown Station"
  },
  {
    "directionId": 1,
    "directionName": "Direction 1",
    "tripHeadsign": "49 UBC"
  }
]
```

---

### Get Route Stops

Get ordered list of stops for a specific route and direction.

**URL:** `/routes/{routeId}/directions/{directionId}/stops`  
**Method:** `GET`  
**Path Parameters:**
- `routeId`: The route ID
- `directionId`: The direction ID (0 or 1)

**Example Request:**
```http
GET /api/routes/6636/directions/0/stops
```

**Sample Response:**
```json
[
  {
    "stopId": "12345",
    "stopName": "Metrotown Station Bay 1", 
    "stopSequence": 1,
    "latitude": 49.2268,
    "longitude": -123.0118
  },
  {
    "stopId": "12346",
    "stopName": "Imperial St @ Kingsway",
    "stopSequence": 2, 
    "latitude": 49.2301,
    "longitude": -123.0156
  },
  {
    "stopId": "1989",
    "stopName": "Westbound E 49 Ave @ Prince Edward St",
    "stopSequence": 3,
    "latitude": 49.2301, 
    "longitude": -123.0987
  }
]
```

---

## Real-Time Data Endpoints

### Get Stop Arrivals

**THE MAIN ENDPOINT** - Get real-time and scheduled bus arrivals for a specific stop.

**URL:** `/routes/{routeId}/directions/{directionId}/stops/{stopId}/arrivals`  
**Method:** `GET`  
**Path Parameters:**
- `routeId`: The route ID
- `directionId`: The direction ID (0 or 1)  
- `stopId`: The stop ID (from stop search or stop list)

**Features:**
- Real-time vehicle positions with ETA calculations
- Scheduled arrivals filtered by active services only
- Shape-based distance calculations for accuracy
- Service calendar validation (shows only buses actually running today)

**Example Request:**
```http
GET /api/routes/6636/directions/0/stops/1989/arrivals
```

**Sample Response:**
```json
{
  "route": {
    "routeShortName": "049",
    "directionName": "Towards Metrotown"
  },
  "stop": {
    "stopId": "1989", 
    "stopName": "Westbound E 49 Ave @ Prince Edward St"
  },
  "realTimeBuses": [
    {
      "vehicleId": "18394",
      "tripId": "14533627", 
      "etaMinutes": 4,
      "etaSeconds": 243,
      "distanceMeters": 850.0,
      "currentStatus": "IN_TRANSIT_TO",
      "lastUpdated": "2025-06-28T10:30:15Z",
      "scheduledArrival": "10:34:00",
      "delayMinutes": -1,
      "delayStatus": "Early"
    },
    {
      "vehicleId": "18395",
      "tripId": "14533628",
      "etaMinutes": 12, 
      "etaSeconds": 720,
      "distanceMeters": 2100.0,
      "currentStatus": "IN_TRANSIT_TO", 
      "lastUpdated": "2025-06-28T10:30:10Z",
      "scheduledArrival": "10:42:00",
      "delayMinutes": 0,
      "delayStatus": "On Time"
    }
  ],
  "scheduledBuses": [
    {
      "scheduledArrival": "10:45:00",
      "etaMinutes": 15,
      "realTime": false
    },
    {
      "scheduledArrival": "10:55:00", 
      "etaMinutes": 25,
      "realTime": false
    },
    {
      "scheduledArrival": "11:05:00",
      "etaMinutes": 35, 
      "realTime": false
    }
  ]
}
```

**Field Descriptions:**

**Real-Time Bus Fields:**
- `vehicleId`: Unique vehicle identifier
- `tripId`: GTFS trip ID for this journey
- `etaMinutes`/`etaSeconds`: Estimated time of arrival
- `distanceMeters`: Distance from vehicle to stop (shape-based calculation)
- `currentStatus`: Vehicle status (IN_TRANSIT_TO, STOPPED_AT, etc.)
- `lastUpdated`: When vehicle position was last updated
- `scheduledArrival`: Scheduled arrival time (if available)
- `delayMinutes`: Minutes early (-) or late (+) vs schedule
- `delayStatus`: "Early", "On Time", "Late", or null

**Scheduled Bus Fields:**
- `scheduledArrival`: Time bus is scheduled to arrive
- `etaMinutes`: Minutes until scheduled arrival
- `realTime`: Always false for scheduled-only buses

---

### Get Route Vehicle Stats

Get live vehicle statistics for a route (bonus endpoint for monitoring).

**URL:** `/routes/{routeId}/vehicles`  
**Method:** `GET`  
**Path Parameters:**
- `routeId`: The route ID

**Example Request:**
```http
GET /api/routes/6636/vehicles
```

**Sample Response:**
```json
{
  "totalVehicles": 12,
  "freshVehicles": 12,
  "direction0Count": 6,
  "direction1Count": 6
}
```

---

## Error Responses

All endpoints return standard HTTP status codes with JSON error responses:

**400 Bad Request:**
```json
{
  "error": "Query parameter 'q' is required and must be at least 1 character"
}
```

**404 Not Found:**
```json
{
  "error": "Route not found",
  "routeId": "invalid-route-id"
}
```

**500 Internal Server Error:**
```json
{
  "error": "Internal server error occurred"
}
```

---

## Common Usage Patterns

### 1. Search and Select Route
```http
# 1. Search for route
GET /api/search/routes?q=49

# 2. Get directions for selected route
GET /api/routes/6636/directions

# 3. Get stops for selected direction  
GET /api/routes/6636/directions/0/stops
```

### 2. Search and Select Stop within Route
```http
# 1. Search for stop within route/direction
GET /api/search/stops?q=Princ&routeId=6636&directionId=0

# 2. Get arrivals for selected stop
GET /api/routes/6636/directions/0/stops/1989/arrivals
```

### 3. Full User Flow Example
```http
# User searches "49"
GET /api/search/routes?q=49
# → Returns routeId: "6636"

# User selects route, gets directions
GET /api/routes/6636/directions  
# → Returns directionId: 0 "Towards Metrotown"

# User searches for "Prince" stops on route 49 direction 0
GET /api/search/stops?q=Prince&routeId=6636&directionId=0
# → Returns stopId: "1989"

# User gets real-time arrivals
GET /api/routes/6636/directions/0/stops/1989/arrivals
# → Returns real-time and scheduled buses
```

---

## Key Features

### Smart Route Search
- **Padded Numbers**: "2" finds "002", "49" finds "049"
- **Fuzzy Matching**: Partial names work
- **Relevance Scoring**: Best matches first

### Route-Specific Stop Search  
- **Scoped Results**: Only stops served by chosen route/direction
- **Fast Filtering**: Pre-filtered at repository level
- **Geographic Info**: Includes coordinates for mapping

### Real-Time Accuracy
- **Shape-Based Distance**: Uses actual route paths, not straight-line
- **Service Calendar Validation**: Only shows buses actually running today
- **Meaningful Results**: Filters out buses essentially at stops (< 50m or < 30s)

### Production Ready
- **Error Handling**: Comprehensive validation and error responses
- **Performance**: Optimized queries with configurable limits
- **Logging**: Detailed logging for monitoring and debugging
- **CORS Enabled**: Ready for frontend integration

This API provides everything needed to build a complete real-time transit tracking frontend application!