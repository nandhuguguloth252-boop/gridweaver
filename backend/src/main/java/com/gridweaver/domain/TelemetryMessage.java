package com.gridweaver.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryMessage(
        @JsonAlias({"nodeId", "deviceId", "id"})
        String nodeId,

        @JsonAlias({"solarKw", "power", "solar", "generation"})
        double solarKw,

        @JsonAlias({"loadKw", "gridLoad", "load", "demand"})
        double loadKw,

        @JsonAlias({"chargeKwh", "batteryLevel", "battery", "charge"})
        double chargeKwh,

        @JsonAlias({"health"})
        Double health,

        @JsonAlias({"temperature", "temp"})
        Double temperature,

        @JsonAlias({"timestamp", "time"})
        Instant timestamp
) {
    public TelemetryMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (health == null && temperature != null) {
            health = temperature > 60.0 ? 15.0 : Math.max(25.0, 100.0 - (temperature - 25.0));
        } else if (health == null) {
            health = 95.0;
        }
        if (temperature == null) {
            temperature = Math.max(20.0, 75.0 - (health * 0.5));
        }
    }

    public TelemetryMessage(String nodeId, double solarKw, double loadKw, double chargeKwh, double health, Instant timestamp) {
        this(nodeId, solarKw, loadKw, chargeKwh, health, null, timestamp);
    }

    public double effectiveHealth() {
        if (temperature != null && temperature > 60.0) {
            return 15.0;
        }
        return health != null ? health : 90.0;
    }
}
