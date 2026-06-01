package org.example.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@AllArgsConstructor
@Builder
@Jacksonized
@Getter
public class Readings {
    private final Double voltage;
    private final Double frequency;
    private final Double activePower;
    private final Double reactivePower;
    private final Double irradiance;
    private final Double panelTemperature;
    private final Double windSpeed;
    private final Double rotorSpeed;
    private final Double stateOfCharge;
    private final Double chargingPower;
    private final Double batteryLevel;
    private final Double chargeRate;
    private final Map<String, Double> additionalMetrics;
}
