package org.example.gateway.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.config.KafkaRoutingProperties;
import org.example.gateway.model.TelemetryPayload;
import org.example.gateway.value.DeviceType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Slf4j
public class TelemetryRoutingService {

    private final KafkaTemplate<String, TelemetryPayload> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final KafkaRoutingProperties kafkaRoutingProperties;

    public void route(TelemetryPayload payload) {
        String topic = resolveTopic(payload.getDeviceType());
        String key = payload.getDeviceId();
        log.debug("Routing telemetry: device={}, topic={}", payload.getDeviceId(), topic);
        log.info("Sending to topic {}", topic);
        CompletableFuture<SendResult<String, TelemetryPayload>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                onSuccess(payload, result);
            } else {
                onFailure(payload, topic, ex);
            }
        });
    }

    private void onSuccess(TelemetryPayload payload, SendResult<String, TelemetryPayload> result) {
        log.debug("Telemetry sent: deviceId={}, partition={}, offset={}", payload.getDeviceId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        meterRegistry.counter("telemetry.sent",
                "deviceType", payload.getDeviceType().name(), "topic", resolveTopic(payload.getDeviceType())).increment();
    }

    private void onFailure(TelemetryPayload payload, String originalTopic, Throwable ex) {
        log.error("Error sending telemetry to topic = {}, deviceId = {}: {}", originalTopic, payload.getDeviceId(), ex.getMessage());
        sendToDlq(payload);
    }

    private void sendToDlq(TelemetryPayload payload) {
        String dlqTopic = kafkaRoutingProperties.getDlq();
        log.debug("Sending to dlq");
        try {
            kafkaTemplate.send(dlqTopic, payload.getDeviceId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Cannot send to DLQ! deviceId={}", payload.getDeviceId());
                        } else {
                            log.warn("Telemetry sent do DLQ: deviceId={}", payload.getDeviceId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error during sending DLQ for deviceId={}: {}", payload.getDeviceId(), e.getMessage());
        }
    }

    private String resolveTopic(DeviceType deviceType) {
        String topic = kafkaRoutingProperties.getRouting().get(deviceType);
        if (topic == null) {
            log.warn("Configuration for deviceType : {} not found, routing to DLQ", deviceType);
            return kafkaRoutingProperties.getDlq();
        }
        return topic;
    }
}
