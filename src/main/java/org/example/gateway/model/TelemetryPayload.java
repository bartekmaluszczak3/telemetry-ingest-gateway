package org.example.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.example.gateway.value.DeviceType;

import java.time.Instant;

@Jacksonized
@Builder
@AllArgsConstructor
@Getter
public class TelemetryPayload {
    private final String deviceId;
    private final DeviceType deviceType;
    private final Instant timestamp;
    private final Readings readings;
    private String gridZone;
    private Instant receivedAt;
}
