# Real-Time Transit System

A real-time vehicle tracking system that ingests GTFS data from TransLink's API and streams it through Kafka for real-time vehicle position tracking.

## Project Structure

```
├── README.md                    # This file
├── pom.xml                     # Maven parent configuration
├── .env                        # Environment variables (not in repo)
├── google_transit.zip          # GTFS static data
├── config/                     # Configuration templates
│   ├── .env.template          # Environment variables template
│   └── application-template.yml
├── deployment/                 # Docker and deployment files
│   ├── docker-compose.yml     # Base compose with Kafka
│   ├── docker-compose.local.yml   # Local development overlay
│   ├── docker-compose.cloud.yml   # Confluent Cloud overlay
│   ├── docker-compose.base.yml    # Services only (no Kafka)
│   ├── docker-compose.prod.yml    # Production deployment
│   ├── start.sh               # Deployment script
│   ├── setup.sh               # Setup script
│   └── nginx/                 # Nginx configuration
├── docs/                      # Documentation
│   ├── DEPLOYMENT.md          # Comprehensive deployment guide
│   ├── API_DOCUMENTATION.md   # API documentation
│   └── DEPLOYMENT_GUIDE.md    # Additional deployment info
├── data-ingestion-service/    # Data ingestion microservice
├── transit-tracker-service/   # Transit tracking microservice
└── shared-models/             # Common models and protobuf
```

## Architecture

The system consists of 3 main modules:

1. **shared-models**: Common data models and protobuf definitions for GTFS data
2. **data-ingestion-service**: Spring Boot service that polls TransLink GTFS API every 30 seconds and publishes vehicle positions to Kafka (runs on port 8081)
3. **transit-tracker-service**: Spring Boot service that consumes Kafka events, loads GTFS static data, and provides REST API with WebSocket support (runs on port 8082)

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker and Docker Compose
- TransLink API key ([Get one here](https://developer.translink.ca/))
- (Optional) Confluent Cloud account for production

### 1. Environment Setup
```bash
# Clone repository
git clone <your-repo-url>
cd real-time-transit

# Copy environment template and fill in your values
cp config/.env.template .env
# Edit .env with your actual API keys
```

### 2. Local Development (with containerized Kafka)
```bash
# Start everything locally with Kafka UI
cd deployment
./start.sh local up

# Services will be available at:
# - Data Ingestion Service: http://localhost:8081
# - Transit Tracker Service: http://localhost:8082  
# - Kafka UI: http://localhost:8080
```

### 3. Testing with Confluent Cloud
```bash
# Use your Confluent Cloud credentials from .env
cd deployment
./start.sh cloud up
```

### 4. Check Service Health
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## Development Workflows

### Local Development
- Uses containerized Kafka (no external dependencies)
- Kafka UI available for monitoring
- Fast polling intervals (10 seconds) for development
- All logs visible via `./start.sh local logs`

### Cloud Testing  
- Uses your Confluent Cloud Kafka
- Production-like configuration
- Tests integration with managed Kafka

### Building and Testing
```bash
# Build all modules
mvn clean compile

# Run tests
mvn test

# Package for deployment
mvn clean package
```

## API Endpoints

### Data Ingestion Service (Port 8081)
- `GET /health` - Health check
- `GET /stats` - Ingestion statistics

### Transit Tracker Service (Port 8082)
- `GET /api/routes/{routeId}/directions/{directionId}/arrivals` - Get arrival predictions
- `GET /health` - Health check
- `WS /ws` - WebSocket for real-time updates

## Monitoring

### Local Development
- **Kafka UI**: http://localhost:8080 (monitor topics, consumers, messages)
- **Application Health**: Check `/health` endpoints
- **Logs**: `./start.sh local logs` or `./start.sh local logs | grep [service-name]`

### Production
- Health checks built into Docker Compose
- Structured logging with rotation
- Metrics available via actuator endpoints

## Deployment Options

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for comprehensive deployment documentation.

### Quick Commands
```bash
cd deployment

# Local development with full Kafka stack
./start.sh local up

# Production-like testing with Confluent Cloud  
./start.sh cloud up

# Full production deployment
./start.sh prod up

# View logs
./start.sh [env] logs

# Stop services
./start.sh [env] down
```

## Configuration

The system supports multiple environment profiles:

- **local**: Development with containerized Kafka
- **prod**: Production with Confluent Cloud and security

Configuration files are in each service's `src/main/resources/`:
- `application.yml` - Base configuration
- `application-local.yml` - Local development overrides  
- `application-prod.yml` - Production configuration with security

## Troubleshooting

### Common Issues

1. **Services won't start**: Check `.env` file has valid credentials
2. **Kafka connection failed**: Verify `KAFKA_BOOTSTRAP_SERVERS` and credentials
3. **TransLink API errors**: Check `TRANSLINK_API_KEY` is valid
4. **Port conflicts**: Ensure ports 8080, 8081, 8082, 9092 are available

### Getting Help
```bash
# Check service status
cd deployment && ./start.sh local status

# View detailed logs  
cd deployment && ./start.sh local logs

# Test Kafka connectivity (if using local Kafka)
docker exec -it transit-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic vehicle-positions
```

## Contributing

1. Follow the modular architecture
2. Use Spring profiles for environment-specific config
3. Add tests for new functionality  
4. Update documentation for API changes
5. Use the provided Docker setup for consistent environments

## License

[Your License]