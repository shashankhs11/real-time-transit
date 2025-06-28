package com.bustracker.ingestion.service;

import com.bustracker.shared.model.VehiclePosition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class VehiclePositionProducer {

  private static final Logger logger = LoggerFactory.getLogger(VehiclePositionProducer.class);

  private final KafkaTemplate<String, byte[]> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topicName;

  @Autowired
  public VehiclePositionProducer(KafkaTemplate<String, byte[]> kafkaTemplate,
      @Value("${kafka.topics.vehicle-positions}") String topicName) {
    this.kafkaTemplate = kafkaTemplate;
    this.topicName = topicName;
    this.objectMapper = new ObjectMapper();

    logger.info("VehiclePositionProducer initialized for topic: {}", topicName);
  }

  /**
   * Publish a list of vehicle positions to Kafka
   */
  public CompletableFuture<Void> publishVehiclePositions(List<VehiclePosition> vehiclePositions) {
    logger.debug("Publishing {} vehicle positions to Kafka topic: {}",
        vehiclePositions.size(), topicName);

    // Create a list of futures for all publish operations
    List<CompletableFuture<SendResult<String, byte[]>>> futures = vehiclePositions.stream()
        .map(this::publishSingleVehiclePosition)
        .toList();

    // Combine all futures into one
    CompletableFuture<Void> allFutures = CompletableFuture.allOf(
        futures.toArray(new CompletableFuture[0])
    );

    return allFutures.whenComplete((result, throwable) -> {
      if (throwable != null) {
        logger.error("Failed to publish some vehicle positions", throwable);
      } else {
        logger.debug("Successfully published all {} vehicle positions", vehiclePositions.size());
      }
    });
  }

  /**
   * Publish a single vehicle position to Kafka
   */
  public CompletableFuture<SendResult<String, byte[]>> publishSingleVehiclePosition(VehiclePosition vehiclePosition) {
    try {
      // Use vehicleId as the message key for partitioning
      String messageKey = vehiclePosition.getVehicleId();

      // Serialize the vehicle position to JSON bytes
      byte[] messageValue = objectMapper.writeValueAsBytes(vehiclePosition);

      logger.debug("Publishing vehicle {} to topic {}",
          vehiclePosition.getVehicleId(), topicName);

      // Send to Kafka
      CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(
          topicName, messageKey, messageValue
      );

      // Add success/failure callbacks
      future.whenComplete((result, ex) -> {
        if (ex != null) {
          logger.error("Failed to publish vehicle position for vehicle {}: {}",
              vehiclePosition.getVehicleId(), ex.getMessage());
        } else {
          logger.debug("Successfully published vehicle {} to partition {} offset {}",
              vehiclePosition.getVehicleId(),
              result.getRecordMetadata().partition(),
              result.getRecordMetadata().offset());
        }
      });

      return future;

    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize vehicle position for vehicle {}: {}",
          vehiclePosition.getVehicleId(), e.getMessage());

      // Return a failed future
      CompletableFuture<SendResult<String, byte[]>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(e);
      return failedFuture;
    }
  }

  /**
   * Test method to publish a single test message
   */
  public CompletableFuture<SendResult<String, byte[]>> publishTestMessage() {
    logger.debug("Publishing test message to Kafka topic: {}", topicName);

    // Create a test vehicle position
    VehiclePosition testVehicle = new VehiclePosition();
    testVehicle.setVehicleId("TEST-VEHICLE-001");
    testVehicle.setTripId("TEST-TRIP-001");
    testVehicle.setRouteId("TEST-ROUTE");
    testVehicle.setLatitude(49.2827);  // Vancouver coordinates
    testVehicle.setLongitude(-123.1207);
    testVehicle.setCurrentStatus("IN_TRANSIT_TO");
    testVehicle.setTimestamp(System.currentTimeMillis() / 1000);

    return publishSingleVehiclePosition(testVehicle);
  }

  /**
   * Get producer metrics for monitoring
   */
  public void logProducerMetrics() {
    // Log basic producer information
    logger.info("Kafka Producer Metrics:");
    logger.info("  Topic: {}", topicName);
    logger.info("  Template: {}", kafkaTemplate.getClass().getSimpleName());
  }
}