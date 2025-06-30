package com.bustracker.ingestion.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.sasl.jaas.config:#{null}}")
  private String jaasConfig;

  @Value("${spring.kafka.security.protocol:PLAINTEXT}")
  private String securityProtocol;

  @Value("${spring.kafka.sasl.mechanism:#{null}}")
  private String saslMechanism;

  @Bean
  public ProducerFactory<String, byte[]> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    // Basic Kafka configuration
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class);

    // Reliability configuration
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

    // Security configuration - only add if not PLAINTEXT
    configProps.put("security.protocol", securityProtocol);
    if (!"PLAINTEXT".equals(securityProtocol) && saslMechanism != null) {
      configProps.put("sasl.mechanism", saslMechanism);
      if (jaasConfig != null) {
        configProps.put("sasl.jaas.config", jaasConfig);
      }
    }

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, byte[]> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}