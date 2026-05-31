package org.example.gateway.service;
import lombok.SneakyThrows;
import org.example.gateway.exception.DeviceRegistryException;
import org.example.gateway.value.DeviceInfo;
import org.example.gateway.value.DeviceStatus;
import org.example.gateway.value.DeviceType;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DeviceRegistryServiceTest extends DeviceRegistryBase{

    @SneakyThrows
    @Test
    void shouldReturnDeviceInfo() {
        // given
        DeviceInfo deviceInfo = activeDevice("meter-001", DeviceType.SMART_METER);
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(deviceInfo))));

        // when
        DeviceInfo result = deviceRegistryService.getRegisteredDevice("meter-001");

        // then
        assertThat(result.deviceId()).isEqualTo("meter-001");
        assertThat(result.deviceType()).isEqualTo(DeviceType.SMART_METER);
        assertThat(result.deviceStatus()).isEqualTo(DeviceStatus.ACTIVE);

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/v1/devices/meter-001")));
    }

    @Test
    @SneakyThrows
    void shouldReturnTrueWhenDeviceIsActive() {
        // given
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/solar-042"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(activeDevice("solar-042", DeviceType.SOLAR_PANEL)))));

        // when and then
        assertThat(deviceRegistryService.isRegisteredAndActive("solar-042")).isTrue();
    }

    @Test
    void shouldThrowErrorWhenDeviceIsNotRegistered() {
        // given
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/unknown-999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Device not found\"}")));

        // when and then
        assertThatThrownBy(() -> deviceRegistryService.getRegisteredDevice("unknown-999"))
                .isInstanceOf(DeviceRegistryException.class)
                .hasMessageContaining("Error during sending request to get device info");
    }

    @SneakyThrows
    @Test
    void shouldThrowErrorWhenDeviceIsSuspended() {
        // given
        DeviceInfo suspended = new DeviceInfo(
                "meter-001", DeviceType.SMART_METER, DeviceStatus.SUSPENDED, "owner-1", "ZONE_A");
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(suspended))));

        // when and then
        assertThatThrownBy(() -> deviceRegistryService.getRegisteredDevice("meter-001"))
                .isInstanceOf(DeviceRegistryException.class)
                .hasMessageContaining("SUSPENDED");
    }

    @SneakyThrows
    @Test
    void shouldThrowErrorWhenDeviceIsDecommissioned() {
        // given
        DeviceInfo suspended = new DeviceInfo(
                "meter-001", DeviceType.SMART_METER, DeviceStatus.DECOMMISSIONED, "owner-1", "ZONE_A");
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(suspended))));

        // when and then
        assertThatThrownBy(() -> deviceRegistryService.getRegisteredDevice("meter-001"))
                .isInstanceOf(DeviceRegistryException.class)
                .hasMessageContaining("DECOMMISSIONED");
    }

    @Test
    void shouldReturnFalseWhenDeviceIsNotRegistered() {
        // given
        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/ghost-device"))
                .willReturn(aResponse().withStatus(404)));

        // when and then
        assertThat(deviceRegistryService.isRegisteredAndActive("ghost-device")).isFalse();
    }
}
