package com.bustracker.ingestion.service;

import com.bustracker.ingestion.config.TransLinkProperties;
import com.bustracker.shared.gtfs.GtfsRealtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class TransLinkApiClient {

  private static final Logger logger = LoggerFactory.getLogger(TransLinkApiClient.class);

  private final WebClient webClient;
  private final TransLinkProperties properties;

  @Autowired
  public TransLinkApiClient(TransLinkProperties properties) {
    this.properties = properties;
    this.webClient = WebClient.builder()
        .baseUrl(properties.getApi().getBaseUrl())
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer
        .build();

    logger.info("TransLink API Client initialized with base URL: {}", properties.getApi().getBaseUrl());
  }

  /**
   * Fetch vehicle positions from TransLink GTFS API
   * @return Mono containing parsed GTFS FeedMessage
   */
  public Mono<GtfsRealtime.FeedMessage> fetchVehiclePositions() {
    String endpoint = properties.getApi().getEndpoints().getPositions();
    String apiKey = properties.getApi().getApiKey();

    logger.debug("Fetching vehicle positions from: {}{}", properties.getApi().getBaseUrl(), endpoint);

    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(endpoint)
            .queryParam("apikey", apiKey)
            .build())
        .retrieve()
        .bodyToMono(byte[].class)
        .doOnNext(responseBytes -> {
          // Add a breakpoint here or log
          logger.debug("Received response bytes: {}", responseBytes.length);
          // You can also inspect 'responseBytes' in the debugger
        })
        .flatMap(this::parseProtobufResponse)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
            .filter(this::isRetryableException))
        .doOnSuccess(feedMessage -> {
          if (feedMessage != null) {
            logger.debug("Successfully fetched {} vehicle entities",
                feedMessage.getEntityCount());
          }
        })
        .doOnError(error -> logger.error("Error fetching vehicle positions: {}", error.getMessage()));
  }

  /**
   * Parse raw protobuf bytes into GTFS FeedMessage
   */
  private Mono<GtfsRealtime.FeedMessage> parseProtobufResponse(byte[] responseBytes) {
    return Mono.fromCallable(() -> {
      try {
        logger.debug("Parsing protobuf response, size: {} bytes", responseBytes.length);

        GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(responseBytes);

        logger.debug("Successfully parsed GTFS feed - Version: {}, Entities: {}, Timestamp: {}",
            feedMessage.getHeader().getGtfsRealtimeVersion(),
            feedMessage.getEntityCount(),
            feedMessage.getHeader().hasTimestamp() ? feedMessage.getHeader().getTimestamp() : "N/A");

        return feedMessage;

      } catch (Exception e) {
        logger.error("Failed to parse protobuf response", e);
        throw new RuntimeException("Failed to parse GTFS protobuf data", e);
      }
    });
  }

  /**
   * Determine if an exception should trigger a retry
   */
  private boolean isRetryableException(Throwable throwable) {
    if (throwable instanceof WebClientResponseException webException) {
      int statusCode = webException.getStatusCode().value();

      // Retry on server errors (5xx) but not client errors (4xx)
      boolean shouldRetry = statusCode >= 500 && statusCode < 600;
      logger.warn("HTTP {} error - Will retry: {}", statusCode, shouldRetry);
      return shouldRetry;
    }

    // Retry on network/timeout issues
    return true;
  }

  /**
   * Test method to validate API connectivity
   */
  public Mono<Boolean> testConnection() {
    logger.info("Testing TransLink API connection...");

    return fetchVehiclePositions()
        .map(feedMessage -> {
          boolean isValid = feedMessage != null && feedMessage.getHeader() != null;
          logger.info("API connection test result: {}", isValid ? "SUCCESS" : "FAILED");
          return isValid;
        })
        .onErrorReturn(false);
  }
}
