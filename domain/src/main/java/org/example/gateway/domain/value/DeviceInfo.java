package org.example.gateway.domain.value;

public record DeviceInfo (
        String deviceId,
        DeviceType deviceType,
        DeviceStatus deviceStatus,
        String ownerId,
        String gridZone
) {
}
