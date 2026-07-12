package org.example.gateway.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.utils.KafkaMockConsumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class TelemetryRoutingServiceTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @Autowired
    private TelemetryRoutingService routingService;

    private KafkaMockConsumer kafkaMockConsumer;

    @BeforeEach
    void setUp() {
        kafkaMockConsumer = new KafkaMockConsumer(kafkaContainer.getBootstrapServers());
    }

    @AfterEach
    void tearDown() {
        kafkaMockConsumer.close();
    }

    @Test
    void shouldRouteSmartMeter() {
        // given
        TelemetryPayload payload = buildPayload("meter-001", DeviceType.SMART_METER);
        kafkaMockConsumer.subscribe(List.of("telemetry.meters"));

        // when
        routingService.route(payload);
        ConsumerRecord<String, TelemetryPayload> record = kafkaMockConsumer.pollSingle();
        Assertions.assertEquals("meter-001", record.key());
        Assertions.assertEquals(DeviceType.SMART_METER, record.value().getDeviceType());
        Assertions.assertEquals(record.topic(), "telemetry.meters");
    }

    @Test
    void shouldRouteSolar() {
        TelemetryPayload payload = buildPayload("solar-001", DeviceType.SOLAR_PANEL);
        kafkaMockConsumer.subscribe(List.of("telemetry.solar"));

        routingService.route(payload);

        ConsumerRecord<String, TelemetryPayload> record = kafkaMockConsumer.pollSingle();
        Assertions.assertEquals("solar-001", record.key());
        Assertions.assertEquals(DeviceType.SOLAR_PANEL, record.value().getDeviceType());
        Assertions.assertEquals(record.topic(), "telemetry.solar");
    }
    @Test
    void shouldRouteBattery() {
        TelemetryPayload payload = buildPayload("battery-001", DeviceType.BATTERY_STORAGE);
        kafkaMockConsumer.subscribe(List.of("telemetry.battery"));

        routingService.route(payload);

        ConsumerRecord<String, TelemetryPayload> record = kafkaMockConsumer.pollSingle();
        Assertions.assertEquals("battery-001", record.key());
        Assertions.assertEquals(DeviceType.BATTERY_STORAGE, record.value().getDeviceType());
        Assertions.assertEquals(record.topic(), "telemetry.battery");
    }
    @Test
    void shouldRouteWindTurbine() {
        TelemetryPayload payload = buildPayload("wind-001", DeviceType.WIND_TURBINE);
        kafkaMockConsumer.subscribe(List.of("telemetry.wind"));

        routingService.route(payload);

        ConsumerRecord<String, TelemetryPayload> record = kafkaMockConsumer.pollSingle();
        Assertions.assertEquals("wind-001", record.key());
        Assertions.assertEquals(DeviceType.WIND_TURBINE, record.value().getDeviceType());
        Assertions.assertEquals(record.topic(), "telemetry.wind");
    }

    @Test
    void shouldRouteWindEvCharger() {
        TelemetryPayload payload = buildPayload("charger-001", DeviceType.EV_CHARGER);
        kafkaMockConsumer.subscribe(List.of("telemetry.ev"));

        routingService.route(payload);

        ConsumerRecord<String, TelemetryPayload> record = kafkaMockConsumer.pollSingle();
        Assertions.assertEquals("charger-001", record.key());
        Assertions.assertEquals(DeviceType.EV_CHARGER, record.value().getDeviceType());
        Assertions.assertEquals(record.topic(), "telemetry.ev");
    }

    @Test
    void multipleEventsWithSameDeviceShouldLandOnTheSamePartition() {
        // given
        kafkaMockConsumer.subscribe(List.of("telemetry.meters"));

        // when
        for (int i = 0; i < 5; i++) {
            routingService.route(buildPayload("meter-001", DeviceType.SMART_METER));
        }

        // then
        List<ConsumerRecord<String, TelemetryPayload>> records = kafkaMockConsumer.poll(5);
        long distinctPartitions = records.stream()
                .mapToInt(ConsumerRecord::partition)
                .distinct()
                .count();
        Assertions.assertEquals(1, distinctPartitions);
    }
    private TelemetryPayload buildPayload(String deviceId, DeviceType type) {
        Readings readings = new Readings(
                230.1, 50.0, 3.45, 0.5,
                null, null, null, null,
                null, null, null, null, null
        );
        return new TelemetryPayload(deviceId, type, Instant.now(), readings);
    }


}
