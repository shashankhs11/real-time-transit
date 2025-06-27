# Real-Time Bus Tracker MVP

A real-time vehicle tracking system using GTFS data from TransLink and Kafka event streaming.

## Architecture

- **data-ingestion-service**: Fetches GTFS vehicle positions from TransLink API and publishes to Kafka
- **shared-models**: Common data models and protobuf definitions
- **transit-tracker-service**: (Coming in Day 2) Consumes Kafka events and provides real-time API

## Prerequisites

- Java 17+
- Maven 3.6+
- Confluent Cloud account
- TransLink API key

## Local Development Setup

### 1. Clone Repository
```bash
git clone <your-repo-url>
cd real-time-transit
```

### 2. Set Environment Variables
```bash
export KAFKA_API_KEY="your-confluent-api-key"
export KAFKA_API_SECRET="your-confluent-api-secret"
export TRANSLINK_API_KEY="your-translink-api-key"
```

### 3. Build and Run
```bash
# Build all modules
mvn clean compile

# Run data ingestion service
cd data-ingestion-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Docker Deployment

### Build Docker Image
```bash
cd data-ingestion-service
mvn clean package
docker build -t bus-tracker/data-ingestion:latest .
```

### Run with Docker
```bash
docker run -e KAFKA_API_KEY="your-key" \
           -e KAFKA_API_SECRET="your-secret" \
           -e KAFKA_BOOTSTRAP_SERVERS="your-bootstrap-server" \
           -e TRANSLINK_API_KEY="your-translink-key" \
           -p 8081:8081 \
           real-time-transit/data-ingestion:latest
```

## API Endpoints

- `GET /health` - Service health check
- `GET /stats` - Polling statistics

## Monitoring

- Check Confluent Cloud for message throughput
- Monitor `/stats` endpoint for polling cycles
- Service publishes ~1000 vehicle positions every 30 seconds

## Security Notes

- Use environment variables for all sensitive configuration
- Follow the `.gitignore` patterns strictly