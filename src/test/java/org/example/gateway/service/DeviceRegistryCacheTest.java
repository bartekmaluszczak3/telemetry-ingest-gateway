package org.example.gateway.service;
import lombok.SneakyThrows;
import org.example.gateway.exception.DeviceRegistryException;
import org.example.gateway.value.DeviceInfo;
import org.example.gateway.value.DeviceStatus;
import org.example.gateway.value.DeviceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DeviceRegistryCacheTest extends DeviceRegistryBase {

    @SneakyThrows
    @Test
    void secondCallShouldHitCacheNotWiremock() {
        // given
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(activeDevice("meter-001", DeviceType.SMART_METER)))));

        // when
        deviceRegistryService.getRegisteredDevice("meter-001");
        deviceRegistryService.getRegisteredDevice("meter-001");
        DeviceInfo registeredDevice = deviceRegistryService.getRegisteredDevice("meter-001");

        // then
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/devices/meter-001")));
        Assertions.assertEquals("meter-001", registeredDevice.deviceId());
    }

    @SneakyThrows
    @Test
    void shouldEvictCache() {
        // given
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(activeDevice("meter-001", DeviceType.SMART_METER)))));

        // when
        deviceRegistryService.getRegisteredDevice("meter-001");
        deviceRegistryService.evictDevice("meter-001");
        deviceRegistryService.getRegisteredDevice("meter-001");

        // then
        wireMock.verify(2, getRequestedFor(urlEqualTo("/api/v1/devices/meter-001")));
    }

    @SneakyThrows
    @Test
    void shouldUseCacheForDifferentDeviceId() {
        // given 
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(activeDevice("meter-001", DeviceType.SMART_METER)))));

        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/solar-042"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(activeDevice("solar-042", DeviceType.SOLAR_PANEL)))));
        // when
        deviceRegistryService.getRegisteredDevice("meter-001");
        deviceRegistryService.getRegisteredDevice("meter-001");
        deviceRegistryService.getRegisteredDevice("solar-042");
        deviceRegistryService.getRegisteredDevice("solar-042");

        // then
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/devices/meter-001")));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/devices/solar-042")));
    }

    @SneakyThrows
    @Test
    void shouldNotCacheException() {
        // given
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .inScenario("device-registration")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(404))
                .willSetStateTo("registered"));

        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .inScenario("device-registration")
                .whenScenarioStateIs("registered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(activeDevice("meter-001", DeviceType.SMART_METER)))));

        assertThatThrownBy(() -> deviceRegistryService.getRegisteredDevice("meter-001"))
                .isInstanceOf(DeviceRegistryException.class);

        // then
        DeviceInfo result = deviceRegistryService.getRegisteredDevice("meter-001");
        Assertions.assertEquals(result.deviceStatus(), DeviceStatus.ACTIVE);
        wireMock.verify(2, getRequestedFor(urlEqualTo("/api/v1/devices/meter-001")));
    }
}
