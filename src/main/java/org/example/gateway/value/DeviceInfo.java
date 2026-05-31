package org.example.gateway.value;

public record DeviceInfo (
        String deviceId,
        DeviceType deviceType,
        DeviceStatus deviceStatus,
        String ownerId,
        String gridZone
) {
}
