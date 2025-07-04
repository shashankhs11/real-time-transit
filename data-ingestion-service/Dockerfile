# Multi-stage build for data-ingestion-service
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy parent pom.xml and all modules for dependency resolution
COPY pom.xml ./
COPY shared-models ./shared-models

# Copy all service modules (required for multi-module build)
COPY data-ingestion-service/pom.xml ./data-ingestion-service/pom.xml
COPY data-ingestion-service/src ./data-ingestion-service/src
COPY transit-tracker-service/pom.xml ./transit-tracker-service/pom.xml
COPY transit-tracker-service/src ./transit-tracker-service/src

# Build the application
RUN mvn clean package -pl data-ingestion-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre AS runner

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/data-ingestion-service/target/data-ingestion-service-*.jar app.jar

# Change ownership to app user
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Environment variables with defaults
ENV JAVA_OPTS="-Xmx2g -Xms1g -server" \
    KAFKA_BOOTSTRAP_SERVERS="localhost:9092" \
    KAFKA_API_KEY="" \
    KAFKA_API_SECRET="" \
    TRANSLINK_API_KEY="" \
    POLLING_ENABLED="true" \
    POLLING_INTERVAL_SECONDS="30"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]