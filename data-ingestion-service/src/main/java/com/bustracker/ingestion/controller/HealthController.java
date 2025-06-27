package com.bustracker.ingestion.controller;

import com.bustracker.ingestion.service.VehicleDataPollingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

  @Autowired
  private VehicleDataPollingService pollingService;

  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> health = new HashMap<>();
    health.put("status", "UP");
    health.put("service", "data-ingestion-service");
    health.put("polling", pollingService.getStats());
    return health;
  }

  @GetMapping("/stats")
  public VehicleDataPollingService.PollingStats stats() {
    return pollingService.getStats();
  }
}