package org.example.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.example.gateway.value.DeviceType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
@Getter
@Setter
@ConfigurationProperties(prefix = "gridflow.kafka.topics")
public class KafkaRoutingProperties {
    private Map<DeviceType, String> routing;
    private String dlq = "telemetry.dlq";
}
