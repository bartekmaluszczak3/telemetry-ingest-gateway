package org.example.gateway.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
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

    @JsonCreator
    public Readings(
            @JsonProperty("voltage")           Double voltage,
            @JsonProperty("frequency")         Double frequency,
            @JsonProperty("activePower")       Double activePower,
            @JsonProperty("reactivePower")     Double reactivePower,
            @JsonProperty("irradiance")        Double irradiance,
            @JsonProperty("panelTemperature")  Double panelTemperature,
            @JsonProperty("windSpeed")         Double windSpeed,
            @JsonProperty("rotorSpeed")        Double rotorSpeed,
            @JsonProperty("stateOfCharge")     Double stateOfCharge,
            @JsonProperty("chargingPower")     Double chargingPower,
            @JsonProperty("batteryLevel")      Double batteryLevel,
            @JsonProperty("chargeRate")        Double chargeRate,
            @JsonProperty("additionalMetrics") Map<String, Double> additionalMetrics) {
        this.voltage           = voltage;
        this.frequency         = frequency;
        this.activePower       = activePower;
        this.reactivePower     = reactivePower;
        this.irradiance        = irradiance;
        this.panelTemperature  = panelTemperature;
        this.windSpeed         = windSpeed;
        this.rotorSpeed        = rotorSpeed;
        this.stateOfCharge     = stateOfCharge;
        this.chargingPower     = chargingPower;
        this.batteryLevel      = batteryLevel;
        this.chargeRate        = chargeRate;
        this.additionalMetrics = additionalMetrics;
    }

}
