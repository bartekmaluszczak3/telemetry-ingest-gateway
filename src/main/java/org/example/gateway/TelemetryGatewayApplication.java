package org.example.gateway;

import org.example.gateway.config.KafkaRoutingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy
@EnableConfigurationProperties(KafkaRoutingProperties.class)
public class TelemetryGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryGatewayApplication.class, args);
    }
}