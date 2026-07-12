package org.example.gateway.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.SneakyThrows;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.exception.DeviceRegistryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DeviceRegistryCircuitBreakerTest extends DeviceRegistryBase {

    @Test
    void shouldOpenCircuitWhenServerErrorsAreRepeated() {
        // given
        wireMock.stubFor(get(urlMatching("/api/v1/devices/.*"))
                .willReturn(aResponse().withStatus(500)));
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("device-registry");
        Assertions.assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        // when
        for (int i=0; i<10; i++){
            assertThatThrownBy(() ->
                    deviceRegistryService.getRegisteredDevice("meter-0001")).isInstanceOf(Exception.class);
        }

        // then
        Assertions.assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    @Test
    void shouldOpenCircuitWhenTimeout() {
        // given
        wireMock.stubFor(get(urlMatching("/api/v1/devices/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000)));
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("device-registry");

        // when
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() ->
                    deviceRegistryService.getRegisteredDevice("meter-001")
            ).isInstanceOf(Exception.class);
        }

        // then
        Assertions.assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    @Test
    void shouldTriggerFallSecureWhenCircuitIsOpen() {
        // given
        circuitBreakerRegistry.circuitBreaker("device-registry").transitionToOpenState();
        wireMock.stubFor(get(urlMatching("/api/v1/devices/.*"))
                .willReturn(aResponse().withStatus(200)));

        // when
        wireMock.stubFor(get(urlMatching("/api/v1/devices/.*"))
                .willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> deviceRegistryService.getRegisteredDevice("meter-001"))
                .isInstanceOf(DeviceRegistryException.class)
                .hasMessageContaining("Device Registry unavailable");

        // then
        wireMock.verify(0, getRequestedFor(urlMatching("/api/v1/devices/.*")));
    }

    @SneakyThrows
    @Test
    void shouldCloseCircuitAfterRecovery() {
        // given
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("device-registry");
        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        wireMock.stubFor(get(urlEqualTo("/api/v1/devices/meter-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson(activeDevice("meter-001", DeviceType.SMART_METER)))));

        // when
        for (int i = 0; i < 3; i++) {
            cacheManager.getCache("device-registry").clear();
            deviceRegistryService.getRegisteredDevice("meter-001");
        }

        Assertions.assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        wireMock.verify(3, getRequestedFor(urlEqualTo("/api/v1/devices/meter-001")));
    }
}
