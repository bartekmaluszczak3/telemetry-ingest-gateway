package org.example.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceInfo;
import org.example.gateway.exception.DeviceRegistryException;
import org.example.gateway.service.DeviceRegistryService;
import org.example.gateway.service.TelemetryRoutingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telemetry")
@Slf4j
@RequiredArgsConstructor
public class TelemetryController {
    private final TelemetryRoutingService routingService;
    private final DeviceRegistryService deviceRegistryService;

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> ingest(@RequestBody TelemetryPayload payload, HttpServletRequest request) throws DeviceRegistryException {
        String deviceId = (String) request.getAttribute("deviceId");
        if (!deviceId.equals(payload.getDeviceId())) {
            log.warn("MISMATCH: cert={}, payload={}", deviceId, payload.getDeviceId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            DeviceInfo deviceInfo = deviceRegistryService.getRegisteredDevice(deviceId);
            log.info("Device registered: {}", deviceInfo);
            payload.setGridZone(deviceInfo.gridZone());
        } catch (Exception e) {
            log.error("Device registry error", e);
            throw e;
        }
        routingService.route(payload);
        return ResponseEntity.accepted().build();
    }
}
