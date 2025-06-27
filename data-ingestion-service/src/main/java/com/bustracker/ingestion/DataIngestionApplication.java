package com.bustracker.ingestion;

import com.bustracker.ingestion.config.TransLinkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TransLinkProperties.class)
public class DataIngestionApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataIngestionApplication.class, args);
  }
}