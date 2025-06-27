package com.bustracker.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "translink")
public class TransLinkProperties {

  private Api api = new Api();

  public Api getApi() {
    return api;
  }

  public void setApi(Api api) {
    this.api = api;
  }

  public static class Api {
    private String baseUrl;
    private String apiKey;
    private Endpoints endpoints = new Endpoints();
    private Polling polling = new Polling();

    // Getters and Setters
    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public Endpoints getEndpoints() {
      return endpoints;
    }

    public void setEndpoints(Endpoints endpoints) {
      this.endpoints = endpoints;
    }

    public Polling getPolling() {
      return polling;
    }

    public void setPolling(Polling polling) {
      this.polling = polling;
    }
  }

  public static class Endpoints {
    private String positions;

    public String getPositions() {
      return positions;
    }

    public void setPositions(String positions) {
      this.positions = positions;
    }
  }

  public static class Polling {
    private int intervalSeconds = 30;
    private int initialDelaySeconds = 10;

    public int getIntervalSeconds() {
      return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
      this.intervalSeconds = intervalSeconds;
    }

    public int getInitialDelaySeconds() {
      return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(int initialDelaySeconds) {
      this.initialDelaySeconds = initialDelaySeconds;
    }
  }
}
