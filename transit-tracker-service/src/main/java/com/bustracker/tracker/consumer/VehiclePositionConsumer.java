package com.bustracker.tracker.consumer;

import com.bustracker.shared.model.VehiclePosition;
import com.bustracker.tracker.service.VehicleStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class VehiclePositionConsumer {

  private static final Logger logger = LoggerFactory.getLogger(VehiclePositionConsumer.class);

  private final ObjectMapper objectMapper;
  private final VehicleStorageService storageService;
  private int messageCount = 0;

  @Autowired
  public VehiclePositionConsumer(VehicleStorageService storageService) {
    this.storageService = storageService;
    this.objectMapper = new ObjectMapper();
    this.objectMapper.findAndRegisterModules(); // Handle Java 8 time types
  }

  @KafkaListener(topics = "${kafka.topics.vehicle-positions}")
  public void consumeVehiclePosition(String key, byte[] message) {
    try {
      messageCount++;

      // Parse JSON message to VehiclePosition
      VehiclePosition vehiclePosition = objectMapper.readValue(message, VehiclePosition.class);

      // Store in memory
      storageService.storeVehiclePosition(vehiclePosition);

      // Log every 25th message to avoid spam
      if (messageCount % 25 == 0) {
        logger.info("Consumed & stored message #{}: Vehicle {} on route {} - Storage: {}",
            messageCount,
            vehiclePosition.getVehicleId(),
            vehiclePosition.getRouteId(),
            storageService.getStats());
      }

    } catch (Exception e) {
      logger.error("Error processing message {}: {}", messageCount, e.getMessage());
    }
  }

  public int getMessageCount() {
    return messageCount;
  }
}