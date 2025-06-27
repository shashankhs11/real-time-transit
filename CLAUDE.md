# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a real-time transit tracking system that ingests GTFS (General Transit Feed Specification) data from TransLink's API and streams it through Kafka for real-time vehicle position tracking.

## Architecture

The system consists of 3 main modules:

1. **shared-models**: Common data models and protobuf definitions for GTFS data
2. **data-ingestion-service**: Spring Boot service that polls TransLink GTFS API every 30 seconds and publishes vehicle positions to Kafka (runs on port 8081)
3. **transit-tracker-service**: Spring Boot service that consumes Kafka events, loads GTFS static data, and provides REST API with WebSocket support (runs on port 8082)

### Required Environment Variables
- `KAFKA_API_KEY`: Confluent Cloud API key
- `KAFKA_API_SECRET`: Confluent Cloud API secret
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers URL
- `TRANSLINK_API_KEY`: TransLink GTFS API key

## Key Components

### Data Flow
1. **VehicleDataPollingService** (data-ingestion-service): Scheduled service that polls TransLink API every 30 seconds
2. **VehiclePositionProducer** (data-ingestion-service): Publishes vehicle positions to Kafka topic `vehicle-positions`
3. **VehiclePositionConsumer** (transit-tracker-service): Consumes vehicle positions from Kafka
4. **VehicleCorrelationService** (transit-tracker-service): Correlates real-time positions with GTFS static data

### GTFS Data Loading
The transit-tracker-service loads complete GTFS static data on startup from `google_transit.zip`:
- Routes, stops, trips, stop times, shape points, and direction names
- In-memory repository pattern with `GtfsRepository` and `InMemoryGtfsRepository`
- Uses Apache Commons CSV for parsing GTFS files

### API Endpoints
- Data Ingestion Service (8081): `/health`, `/stats`
- Transit Tracker Service (8082): `/api/routes/{routeId}/directions/{directionId}/arrivals`, health/actuator endpoints
- WebSocket endpoint: `/ws` for real-time updates

## Technology Stack
- Java 17
- Spring Boot 3.2.2+ with Web, WebFlux, WebSockets, Kafka, Actuator
- Apache Kafka (via Confluent Cloud)
- Protocol Buffers for GTFS real-time data
- Maven multi-module project structure
- Docker support

## Important Files
- `google_transit.zip`: GTFS static data file (update path in TransitTrackerApplication.java:32)
- `shared-models/src/main/proto/gtfs-realtime.proto`: Protocol buffer definitions
- Application configs: `data-ingestion-service/src/main/resources/application.yml`, `transit-tracker-service/src/main/resources/application.yml`

## Working Agreement

You are an AI developer assistant helping me build software projects one step at a time. For every task, always break it down into smaller, logical steps before doing any implementation. Do not skip ahead.

For anything related to DESIGNing the system, think like a SENIOR SOFTWARE ENGINEER or even a STAFF SOFTWARE ENGINEER.

Only proceed to the next step when I explicitly ask you to. Do not give code or suggestions for future steps unless asked.

At each step:
- Explain the purpose of the current step clearly.
- Wait for my confirmation before moving on.
- Be concise, clear, and pragmatic in your reasoning and code examples.

Prioritize best practices, modular design, clarity, and minimalism. Assume I care about clean code, long-term maintainability, and full understanding of what is being built.

The goal is to build a modular, production-grade backend that follows solid software design principles (clean architecture, separation of concerns, SOLID principles, proper logging, and documentation).