package com.gridweaver.domain;

import java.time.Instant;

public record BatteryNode(
        String id,
        String zone,
        double lat,
        double lon,
        double capacityKwh,
        double chargeKwh,
        double solarKw,
        double loadKw,
        BatteryState state,
        double health,
        long version,
        Instant updatedAt
) {
    public double stateOfCharge() {
        return capacityKwh == 0 ? 0 : Math.max(0, Math.min(1, chargeKwh / capacityKwh));
    }

    public BatteryNode withTelemetry(double solar, double load, double charge, BatteryState next, double nextHealth) {
        return new BatteryNode(id, zone, lat, lon, capacityKwh,
                Math.max(0, Math.min(capacityKwh, charge)), solar, Math.max(0, load), next,
                Math.max(0, Math.min(100, nextHealth)), version + 1, Instant.now());
    }
}
