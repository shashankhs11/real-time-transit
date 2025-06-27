package com.bustracker.ingestion.service;

import com.bustracker.ingestion.config.TransLinkProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VehicleDataPollingService {

  private static final Logger logger = LoggerFactory.getLogger(VehicleDataPollingService.class);

  private final TransLinkApiClient apiClient;
  private final GtfsDataTransformer transformer;
  private final VehiclePositionProducer producer;
  private final TransLinkProperties properties;

  private boolean isPollingEnabled = true;
  private int pollCount = 0;

  @Autowired
  public VehicleDataPollingService(
      TransLinkApiClient apiClient,
      GtfsDataTransformer transformer,
      VehiclePositionProducer producer,
      TransLinkProperties properties) {
    this.apiClient = apiClient;
    this.transformer = transformer;
    this.producer = producer;
    this.properties = properties;
  }

  @PostConstruct
  public void init() {
    logger.info("VehicleDataPollingService initialized");
    logger.info("Polling interval: {} seconds", properties.getApi().getPolling().getIntervalSeconds());
    logger.info("Initial delay: {} seconds", properties.getApi().getPolling().getInitialDelaySeconds());
  }

  /**
   * Scheduled method that runs every 30 seconds (configurable)
   * Fetches vehicle positions from TransLink API and publishes to Kafka
   */
  @Scheduled(fixedRateString = "#{${translink.api.polling.interval-seconds} * 1000}",
      initialDelayString = "#{${translink.api.polling.initial-delay-seconds} * 1000}")
  public void pollVehiclePositions() {
    if (!isPollingEnabled) {
      logger.debug("Polling is disabled, skipping this cycle");
      return;
    }

    pollCount++;
    logger.info("Starting polling cycle #{}", pollCount);

    try {
      apiClient.fetchVehiclePositions()
          .map(feedMessage -> {
            logger.debug("Received GTFS feed with {} entities", feedMessage.getEntityCount());
            return transformer.transformVehiclePositions(feedMessage);
          })
          .flatMap(vehiclePositions -> {
            logger.info("Transformed {} vehicle positions", vehiclePositions.size());

            if (vehiclePositions.isEmpty()) {
              logger.warn("No vehicle positions found in polling cycle: {}", pollCount);
              return Mono.just(vehiclePositions);
            }

            // Log sample data less frequently and only in DEBUG mode
            if (pollCount % 20 == 0 && logger.isDebugEnabled()) {
              logger.debug("Sample vehicles from poll #{}:", pollCount);
              vehiclePositions.stream()
                  .limit(2)
                  .forEach(vehicle -> logger.debug("  {}", vehicle));
            }

            // Publish to Kafka
            return Mono.fromFuture(producer.publishVehiclePositions(vehiclePositions))
                .thenReturn(vehiclePositions);
          })
          .subscribe(
              vehiclePositions -> {
                logger.info("Poll #{} completed: Published {} vehicles to Kafka",
                    pollCount, vehiclePositions.size());
              },
              error -> {
                logger.error("Poll #{} failed: {}", pollCount, error.getMessage(), error);
                // Continue polling even if one cycle fails
              }
          );

    } catch (Exception e) {
      logger.error("Unexpected error in polling cycle #{}: {}", pollCount, e.getMessage(), e);
    }
  }

  /**
   * Enable or disable polling (useful for testing or maintenance)
   */
  public void setPollingEnabled(boolean enabled) {
    logger.info("Polling {}", enabled ? "enabled" : "disabled");
    this.isPollingEnabled = enabled;
  }

  /**
   * Get current polling statistics
   */
  public PollingStats getStats() {
    return new PollingStats(pollCount, isPollingEnabled,
        properties.getApi().getPolling().getIntervalSeconds());
  }

  /**
   * Simple stats class for monitoring
   */
  public static class PollingStats {
    private final int totalPolls;
    private final boolean enabled;
    private final int intervalSeconds;

    public PollingStats(int totalPolls, boolean enabled, int intervalSeconds) {
      this.totalPolls = totalPolls;
      this.enabled = enabled;
      this.intervalSeconds = intervalSeconds;
    }

    public int getTotalPolls() { return totalPolls; }
    public boolean isEnabled() { return enabled; }
    public int getIntervalSeconds() { return intervalSeconds; }

    @Override
    public String toString() {
      return String.format("PollingStats{polls=%d, enabled=%s, interval=%ds}",
          totalPolls, enabled, intervalSeconds);
    }
  }
}