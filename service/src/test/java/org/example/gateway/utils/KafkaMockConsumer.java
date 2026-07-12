package org.example.gateway.utils;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.gateway.domain.TelemetryPayload;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.*;

public class KafkaMockConsumer {
    private final KafkaConsumer<String, TelemetryPayload> consumer;

    public KafkaMockConsumer(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,  "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TelemetryPayload.class.getName());
        this.consumer = new KafkaConsumer<>(props);
    }

    public void close() {
        consumer.close();
    }

    public List<ConsumerRecord<String, TelemetryPayload>> poll(int expected) {
        List<ConsumerRecord<String, TelemetryPayload>> result = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 10_000;

        while (result.size() < expected && System.currentTimeMillis() < deadline) {
            consumer.poll(Duration.ofMillis(500))
                    .forEach(result::add);
        }

        return result;
    }

    public ConsumerRecord<String, TelemetryPayload>  pollSingle() {
        List<ConsumerRecord<String, TelemetryPayload>> records = poll(1);
        return records.get(0);

    }

    public void subscribe(List<String> strings) {
        this.consumer.subscribe(strings);
    }
}
