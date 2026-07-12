package org.example.gateway.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.example.gateway.domain.value.DeviceType;

import java.time.Instant;

@Getter
@Setter
public class TelemetryPayload {
    private final String deviceId;
    private final DeviceType deviceType;
    private final Instant timestamp;
    private final Readings readings;
    private String gridZone;
    private final Instant receivedAt;

    @JsonCreator
    public TelemetryPayload(
            @JsonProperty("deviceId") String deviceId,
            @JsonProperty("deviceType") DeviceType deviceType,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("readings") Readings readings) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.timestamp = timestamp;
        this.readings = readings;
        this.receivedAt = Instant.now();
    }
}
