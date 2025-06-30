# Real-Time Transit System - Deployment Guide

## Overview

This guide covers the deployment and configuration of the Real-Time Transit System, which supports both local development with containerized Kafka and production deployment with Confluent Cloud.

## Architecture

The system consists of:
- **Data Ingestion Service** (Port 8081): Polls TransLink GTFS API and publishes to Kafka
- **Transit Tracker Service** (Port 8082): Consumes Kafka events and provides REST API with WebSocket support
- **Kafka Infrastructure**: Message broker for real-time data streaming
- **Supporting Services**: Zookeeper, Kafka UI for monitoring

## Configuration Profiles

### 1. Local Development (`local` profile)
- Uses containerized Kafka (no authentication)
- Faster polling intervals for development
- Full observability with Kafka UI
- Reduced memory allocation

### 2. Production (`prod` profile)
- Uses Confluent Cloud with SASL_SSL authentication
- Production-grade logging and monitoring
- Optimized memory settings
- Secure configuration management

## Environment Variables

Create a `.env` file in the project root with the following variables:

```env
# TransLink API Key (required for both environments)
TRANSLINK_API_KEY=your_translink_api_key

# Kafka Configuration (for Confluent Cloud)
KAFKA_BOOTSTRAP_SERVERS=your-kafka-bootstrap-servers
KAFKA_API_KEY=your_kafka_api_key
KAFKA_API_SECRET=your_kafka_api_secret

# Application Configuration
POLLING_ENABLED=true
POLLING_INTERVAL_SECONDS=30

# Java Heap Settings
DATA_INGESTION_JAVA_OPTS=-Xmx1g -Xms512m -server -XX:+UseG1GC
TRANSIT_TRACKER_JAVA_OPTS=-Xmx2g -Xms1g -server -XX:+UseG1GC
```

## Deployment Options

### Option 1: Local Development with Containerized Kafka

**Use Case**: Full local development, testing, debugging

**Command**:
```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml up
```

**What it includes**:
- Zookeeper and Kafka containers
- Kafka UI (http://localhost:8080)
- Both microservices with `local` profile
- No authentication required
- Fast polling (10 seconds) for development

**Services Available**:
- Data Ingestion Service: http://localhost:8081
- Transit Tracker Service: http://localhost:8082
- Kafka UI: http://localhost:8080

### Option 2: Cloud Kafka Testing

**Use Case**: Testing with production-like Kafka setup using Confluent Cloud

**Command**:
```bash
docker-compose -f docker-compose.base.yml -f docker-compose.cloud.yml up
```

**What it includes**:
- Only the microservices (no local Kafka)
- Uses Confluent Cloud Kafka
- Production profiles and security
- Production polling intervals (30 seconds)

**Prerequisites**:
- Valid Confluent Cloud credentials in `.env`
- Kafka topics created in Confluent Cloud

### Option 3: Full Production Deployment

**Use Case**: Production deployment with all infrastructure

**Command**:
```bash
docker-compose -f docker-compose.prod.yml up
```

**What it includes**:
- Production-optimized services
- Nginx reverse proxy
- SSL termination
- Log rotation and monitoring
- Health checks and restart policies

## Service Configuration Details

### Data Ingestion Service Configuration

**Local Profile** (`application-local.yml`):
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    # No security configuration
```

**Production Profile** (`application-prod.yml`):
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: PLAIN
      jaas:
        config: org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_API_KEY}" password="${KAFKA_API_SECRET}";
```

### Transit Tracker Service Configuration

Similar structure with consumer-specific settings and WebSocket configuration.

## Operations Guide

### Starting Services

1. **Local Development**:
   ```bash
   # Start everything locally
   docker-compose -f docker-compose.yml -f docker-compose.local.yml up -d
   
   # View logs
   docker-compose logs -f data-ingestion-service
   docker-compose logs -f transit-tracker-service
   ```

2. **Cloud Testing**:
   ```bash
   # Ensure .env has valid Confluent Cloud credentials
   docker-compose -f docker-compose.base.yml -f docker-compose.cloud.yml up -d
   ```

3. **Production**:
   ```bash
   # Full production stack
   docker-compose -f docker-compose.prod.yml up -d
   ```

### Monitoring and Health Checks

**Health Endpoints**:
- Data Ingestion: `http://localhost:8081/actuator/health`
- Transit Tracker: `http://localhost:8082/actuator/health`

**Kafka UI** (local only): `http://localhost:8080`
- Monitor topics, consumers, and message flow
- View consumer lag and partition details

**Logs**:
```bash
# View application logs
docker-compose logs -f [service-name]

# View Kafka logs
docker-compose logs kafka
```

### Troubleshooting

**Common Issues**:

1. **Services not connecting to Kafka**:
   - Check `KAFKA_BOOTSTRAP_SERVERS` in environment
   - Verify network connectivity
   - Check Kafka container health

2. **Authentication failures with Confluent Cloud**:
   - Verify `KAFKA_API_KEY` and `KAFKA_API_SECRET`
   - Ensure API key has proper permissions
   - Check network connectivity to Confluent Cloud

3. **TransLink API errors**:
   - Verify `TRANSLINK_API_KEY` is valid
   - Check API rate limits
   - Monitor ingestion service logs

**Debugging Commands**:
```bash
# Check container status
docker-compose ps

# View detailed logs
docker-compose logs -f --tail=100 [service-name]

# Connect to Kafka container
docker-compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic vehicle-positions

# Check application metrics
curl http://localhost:8081/actuator/metrics
curl http://localhost:8082/actuator/metrics
```

## Security Considerations

### Local Development
- No authentication for local Kafka
- Open ports for debugging
- Verbose logging enabled

### Production
- SASL_SSL authentication for Kafka
- Restricted health endpoint access
- Log rotation and size limits
- Non-root container users
- SSL termination at nginx

## Scaling and Performance

### Memory Allocation
- **Data Ingestion**: 1GB heap (can handle 30-second polling)
- **Transit Tracker**: 2GB heap (loads full GTFS data in memory)

### Kafka Configuration
- **Local**: Single partition for simplicity
- **Production**: Multiple partitions for scalability
- **Consumer Group**: `transit-tracker-group` for load balancing

### Performance Tuning
```bash
# Monitor JVM performance
docker-compose exec [service] jcmd 1 VM.flags
docker-compose exec [service] jcmd 1 GC.run_finalization

# Check memory usage
docker stats
```

## Data Flow

1. **VehicleDataPollingService** polls TransLink API every 30 seconds
2. **VehiclePositionProducer** publishes protobuf messages to `vehicle-positions` topic
3. **VehiclePositionConsumer** processes messages and correlates with GTFS static data
4. **REST API** and **WebSocket** endpoints provide real-time data to clients

## Next Steps

After deployment:
1. Monitor logs for successful data ingestion
2. Check Kafka UI for message flow
3. Test REST API endpoints
4. Set up monitoring dashboards
5. Configure alerting for service health

## Support

For issues:
1. Check service logs first
2. Verify environment variables
3. Test network connectivity
4. Review Kafka cluster health