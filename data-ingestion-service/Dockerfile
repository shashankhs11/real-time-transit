# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/data-ingestion-service-1.0-SNAPSHOT.jar app.jar

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser #Creates a system group called appuser and assigns it to the appuser group.
RUN chown -R appuser:appuser /app
USER appuser

# Expose port (for health checks)
EXPOSE 8081

## Environment variables with defaults (will be overridden)
#ENV KAFKA_API_KEY=""
#ENV KAFKA_API_SECRET=""
#ENV KAFKA_BOOTSTRAP_SERVERS=""
#ENV TRANSLINK_API_KEY=""

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]