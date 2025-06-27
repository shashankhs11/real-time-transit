package com.bustracker.ingestion.service;

import com.bustracker.shared.model.VehiclePosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehiclePositionProducerTest {

  @Mock
  private KafkaTemplate<String, byte[]> kafkaTemplate;

  private VehiclePositionProducer producer;
  private final String topicName = "test-topic";

  @BeforeEach
  void setUp() {
    producer = new VehiclePositionProducer(kafkaTemplate, topicName);
  }

  @Test
  public void testPublishSingleVehiclePosition() {
    // Arrange
    VehiclePosition vehicle = createTestVehicle();
    SendResult<String, byte[]> mockResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.completedFuture(mockResult);

    when(kafkaTemplate.send(eq(topicName), eq(vehicle.getVehicleId()), any(byte[].class)))
        .thenReturn(future);

    // Act
    CompletableFuture<SendResult<String, byte[]>> result = producer.publishSingleVehiclePosition(vehicle);

    // Assert
    assertNotNull(result);
    assertTrue(result.isDone());
    verify(kafkaTemplate).send(eq(topicName), eq(vehicle.getVehicleId()), any(byte[].class));
  }

  @Test
  public void testCreateTestMessage() {
    // Arrange
    SendResult<String, byte[]> mockResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, byte[]>> future = CompletableFuture.completedFuture(mockResult);

    when(kafkaTemplate.send(eq(topicName), anyString(), any(byte[].class)))
        .thenReturn(future);

    // Act & Assert
    assertDoesNotThrow(() -> producer.publishTestMessage());
    verify(kafkaTemplate).send(eq(topicName), anyString(), any(byte[].class));
  }

  private VehiclePosition createTestVehicle() {
    VehiclePosition vehicle = new VehiclePosition();
    vehicle.setVehicleId("TEST-001");
    vehicle.setTripId("TRIP-001");
    vehicle.setRouteId("ROUTE-001");
    vehicle.setLatitude(49.2827);
    vehicle.setLongitude(-123.1207);
    vehicle.setTimestamp(System.currentTimeMillis() / 1000);
    return vehicle;
  }
}
