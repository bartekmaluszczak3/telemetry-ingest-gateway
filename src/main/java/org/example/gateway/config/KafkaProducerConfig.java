package org.example.gateway.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.example.gateway.model.TelemetryPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public ProducerFactory<String, TelemetryPayload> telemetryProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties());
    }

    private Map<String, Object> producerProperties() {
        return Map.ofEntries(
                Map.entry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,                    bootstrapServers),
                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,                 StringSerializer.class),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,               JsonSerializer.class),
                Map.entry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,                   true),
                Map.entry(ProducerConfig.ACKS_CONFIG,                                 "all"),
                Map.entry(ProducerConfig.RETRIES_CONFIG,                              3),
                Map.entry(ProducerConfig.LINGER_MS_CONFIG,                            5),
                Map.entry(ProducerConfig.BATCH_SIZE_CONFIG,                           16_384),
                Map.entry(ProducerConfig.COMPRESSION_TYPE_CONFIG,                     "snappy"),
                Map.entry(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,                   30_000),
                Map.entry(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,       1)
        );
    }

}
