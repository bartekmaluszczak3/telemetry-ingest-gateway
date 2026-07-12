package org.example.gateway.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.domain.value.DeviceInfo;
import org.example.gateway.domain.value.DeviceStatus;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.exception.DeviceRegistryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceRegistryService {
    private static final String CACHE_NAME = "device-registry";
    private static final String CIRCUIT_BREAKER_NAME = "device-registry";
    private final RestTemplate mtlsRestTemplate;

    @Value("${gridflow.services.device-registry.url}")
    private String deviceRegistryUrl;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    @Cacheable(value = CACHE_NAME, key = "#deviceId")
    public DeviceInfo getRegisteredDevice(String deviceId) throws DeviceRegistryException {
        log.debug("Cache miss for device {}", deviceId);
        DeviceInfo deviceInfo;
        try {
            deviceInfo = mtlsRestTemplate.getForObject(
                    deviceRegistryUrl + "/api/v1/devices/{deviceId}",
                    DeviceInfo.class, deviceId);
        } catch (Exception e){
            throw new DeviceRegistryException("Error during sending request to get device info");
        }

        if (deviceInfo == null) {
            log.error("Device with id {} not found in registry", deviceId);
            throw new DeviceRegistryException("Device with id %s not found in registry".formatted(deviceId));
        }

        if(deviceInfo.deviceStatus() != DeviceStatus.ACTIVE){
            log.error("Device with status other than ACTIVE!");
            throw new DeviceRegistryException("Device %s has status %s".formatted(deviceId, deviceInfo.deviceStatus()));
        }
        return deviceInfo;
    }

    public boolean isRegisteredAndActive(String deviceId) {
        try {
            getRegisteredDevice(deviceId);
            return true;
        } catch (DeviceRegistryException e) {
            return false;
        }
    }
    public DeviceType getDeviceType(String deviceId) throws DeviceRegistryException {
        return getRegisteredDevice(deviceId).deviceType();
    }

    @CacheEvict(value = CACHE_NAME, key = "#deviceId")
    public void evictDevice(String deviceId) {
        log.info("Cache for device '{}' evicted", deviceId);
    }

}
