package org.example.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.example.gateway.value.DeviceInfo;
import org.example.gateway.value.DeviceStatus;
import org.example.gateway.value.DeviceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest
@ActiveProfiles("test")
public abstract class DeviceRegistryBase {
    private static ObjectMapper objectMapper = new ObjectMapper();

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideRegistryUrl(DynamicPropertyRegistry registry) {
        registry.add("gridflow.services.device-registry.url",
                () -> "http://localhost:" + wireMock.getPort());
    }

    @Autowired
    DeviceRegistryService deviceRegistryService;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void resetAll() {
        wireMock.resetAll();
        cacheManager.getCache("device-registry").clear();
        circuitBreakerRegistry.circuitBreaker("device-registry").reset();
    }

    DeviceInfo activeDevice(String deviceId, DeviceType type) {
        return new DeviceInfo(deviceId, type, DeviceStatus.ACTIVE, "owner-1", "ZONE_A");
    }

    static String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}

