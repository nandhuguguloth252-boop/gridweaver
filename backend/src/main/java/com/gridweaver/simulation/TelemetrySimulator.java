package com.gridweaver.simulation;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.service.GridRegistryService;
import com.gridweaver.service.TelemetryGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "grid.simulator.enabled", havingValue = "true", matchIfMissing = true)
public class TelemetrySimulator {
    private final GridRegistryService registry;
    private final TelemetryGateway gateway;
    private final int sampleSize;

    public TelemetrySimulator(
            GridRegistryService registry,
            TelemetryGateway gateway,
            @Value("${grid.simulator.sample-size:180}") int sampleSize) {
        this.registry = registry;
        this.gateway = gateway;
        this.sampleSize = Math.max(10, Math.min(sampleSize, 1000));
    }

    @Scheduled(fixedDelayString = "${grid.simulator.interval-ms:1200}")
    public void tick() {
        int submitted = 0;
        for (BatteryNode n : registry.sample(Math.min(sampleSize, registry.size()))) {
            double wave = Math.sin(System.currentTimeMillis() / 15000.0 + submitted);
            double solar = Math.max(0, n.solarKw() + wave * 4 + ThreadLocalRandom.current().nextDouble(-2, 2));
            double load = Math.max(3, n.loadKw() + ThreadLocalRandom.current().nextDouble(-3, 3));
            gateway.submit(new TelemetryMessage(n.id(), solar, load, n.chargeKwh(), n.health(), Instant.now()));
            submitted++;
        }
    }
}
