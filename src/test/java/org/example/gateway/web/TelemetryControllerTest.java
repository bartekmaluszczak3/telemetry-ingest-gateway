package org.example.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.gateway.model.Readings;
import org.example.gateway.model.TelemetryPayload;
import org.example.gateway.utils.KafkaMockConsumer;
import org.example.gateway.value.DeviceInfo;
import org.example.gateway.value.DeviceStatus;
import org.example.gateway.value.DeviceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public class TelemetryControllerTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().port(1010))
            .build();
    @Autowired
    private RestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaMockConsumer kafkaConsumer;

    @BeforeEach
    void setUp() throws Exception {
        kafkaConsumer = new KafkaMockConsumer(kafka.getBootstrapServers());
        kafkaConsumer.subscribe(List.of("telemetry.meters"));
        configureMtlsRestTemplate();
    }

    @AfterEach
    void tearDown() {
        kafkaConsumer.close();
        wireMock.resetAll();
    }

    @Test
    void shouldReturn202AndPublishToKafka() throws Exception {
        String deviceId = "meter-001";
        TelemetryPayload payload = buildPayload(deviceId, DeviceType.SMART_METER);

        stubDeviceRegistry(deviceId, DeviceType.SMART_METER, "ZONE_A");
        kafkaConsumer.subscribe(List.of("telemetry.meters"));

        stubDeviceRegistry(deviceId, DeviceType.SMART_METER, "ZONE_A");
        kafkaConsumer.subscribe(List.of("telemetry.meters"));

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "https://localhost:" + port + "/api/v1/telemetry",
                payload,
                Void.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ConsumerRecord<String, TelemetryPayload> record = kafkaConsumer.pollSingle();
        assertThat(record.key()).isEqualTo(deviceId);
        assertThat(record.value().getDeviceId()).isEqualTo(deviceId);
        assertThat(record.value().getGridZone()).isEqualTo("ZONE_A");
    }

    @Test
    void shouldReturn401WhenMissingCertificate() {
        // given
        TelemetryPayload payload = buildPayload("meter-001", DeviceType.SMART_METER);
        RestTemplate unsecureTemplate = new RestTemplate();

        // when and then
        try {
            unsecureTemplate.postForEntity(
                    "https://localhost:" + port + "/api/v1/telemetry",
                    payload,
                    Void.class
            );
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("unable to find valid certification path ");
        }
    }

    @Test
    void shouldReturn403WhenDeviceIdMismatch() {
        // given
        TelemetryPayload payload = buildPayload("meter-002", DeviceType.SMART_METER);

        // when and then
        try {
            restTemplate.postForEntity(
                    "https://localhost:" + port + "/api/v1/telemetry",
                    payload,
                    Void.class
            );
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("403");
        }
    }

    @Test
    void shouldReturn400WhenInvalidPayload() {
        // given
        String invalidJson = "{\"deviceId\": \"meter-001\"}";

        // when
        try {
            restTemplate.postForEntity(
                    "https://localhost:" + port + "/api/v1/telemetry",
                    invalidJson,
                    Void.class
            );
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("400");
        }

    }

    private void configureMtlsRestTemplate() throws Exception {
        KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("certs/meter-001-keystore.p12")) {
            clientKeyStore.load(is, "gridflow-secret".toCharArray());
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("certs/ca-truststore.p12")) {
            trustStore.load(is, "gridflow-secret".toCharArray());
        }

        SSLContext sslContext = SSLContextBuilder.create()
                .loadKeyMaterial(clientKeyStore, "gridflow-secret".toCharArray())
                .loadTrustMaterial(trustStore, null)
                .build();

        var connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        this.restTemplate = new RestTemplate(requestFactory);
        this.restTemplate.setMessageConverters(
                List.of(new MappingJackson2HttpMessageConverter(objectMapper))  // ← Użyj aplikacyjnego ObjectMapper
        );
    }

    private TelemetryPayload buildPayload(String deviceId, DeviceType type) {
        Readings readings = new Readings(
                230.1, 50.0, 3.45, 0.5,
                null, null, null, null,
                null, null, null, null, null
        );
        return new TelemetryPayload(deviceId, type, Instant.now(), readings);
    }

    private void stubDeviceRegistry(String deviceId, DeviceType type, String gridZone) throws Exception {
        String responseBody = objectMapper.writeValueAsString(new DeviceInfo(
                deviceId, type, DeviceStatus.ACTIVE, "owner-1", gridZone
        ));

        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/" + deviceId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }
}

