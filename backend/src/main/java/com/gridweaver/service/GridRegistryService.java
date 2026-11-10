package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.BatteryState;
import com.gridweaver.domain.BatteryEvent;
import com.gridweaver.statemachine.BatteryStateMachineFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GridRegistryService {
    private static final List<String> ZONES = List.of("NORTH", "SOUTH", "EAST", "WEST");
    private final Map<String, BatteryNode> nodes = new ConcurrentHashMap<>();
    private final BatteryStateMachineFactory machineFactory;
    private final Random random = new Random(42);
    private final int configuredCount;

    public GridRegistryService(BatteryStateMachineFactory machineFactory,
                               @Value("${grid.node-count:5000}") int configuredCount) {
        this.machineFactory = machineFactory;
        this.configuredCount = Math.max(100, Math.min(configuredCount, 60000));
    }

    @PostConstruct
    public void seed() {
        for (int i = 0; i < configuredCount; i++) {
            addNode(i);
        }
    }

    private void addNode(int i) {
        double baseLat = 26.87 + (i % 100) * 0.0012;
        double baseLon = 75.78 + (i / 100) * 0.0010;
        String zone = ZONES.get(i % ZONES.size());
        double cap = 20 + random.nextDouble() * 80;
        double charge = cap * (0.25 + random.nextDouble() * 0.65);
        BatteryState state = BatteryState.IDLE;
        nodes.put("GW-" + String.format("%05d", i), new BatteryNode(
                "GW-" + String.format("%05d", i), zone, baseLat, baseLon, cap, charge,
                random.nextDouble() * 60, 15 + random.nextDouble() * 40,
                state, 85 + random.nextDouble() * 15, 0, Instant.now()));
    }

    public Collection<BatteryNode> all() { return nodes.values(); }
    public BatteryNode get(String id) { return nodes.get(id); }
    public int size() { return nodes.size(); }

    public BatteryNode register(BatteryNode node) {
        nodes.put(node.id(), node);
        return node;
    }

    public BatteryNode registerOrGet(String id, String zone, double lat, double lon, double capacityKwh, double chargeKwh) {
        return nodes.computeIfAbsent(id, k -> new BatteryNode(
                id,
                zone != null ? zone : ZONES.get(Math.abs(id.hashCode()) % ZONES.size()),
                lat != 0 ? lat : 26.9124,
                lon != 0 ? lon : 75.7873,
                capacityKwh > 0 ? capacityKwh : 100.0,
                chargeKwh >= 0 ? chargeKwh : 50.0,
                20.0,
                20.0,
                BatteryState.IDLE,
                95.0,
                0,
                Instant.now()
        ));
    }

    public BatteryNode updateTelemetry(String id, double solar, double load, double charge, double health) {
        BatteryNode current = nodes.get(id);
        if (current == null) {
            current = registerOrGet(id, null, 26.9124, 75.7873, 100.0, charge);
        }
        var machine = machineFactory.create(current.state());
        BatteryEvent event;
        if (current.state() == BatteryState.FAULT) {
            event = health >= 25 ? BatteryEvent.FAULT_RECOVERED : BatteryEvent.FAULT_DETECTED;
        } else if (health < 20) {
            event = BatteryEvent.FAULT_DETECTED;
        } else if (load >= 60) {
            event = BatteryEvent.DEMAND_SURGE;
        } else if (load > 42) {
            event = BatteryEvent.GRID_LOAD_HIGH;
        } else if (solar > load * 1.25) {
            event = BatteryEvent.SOLAR_SURPLUS;
        } else {
            event = BatteryEvent.GRID_LOAD_NORMAL;
        }
        BatteryState next = machine.getState().getId();
        try {
            next = machineFactory.apply(machine, event);
        } catch (Exception ignored) {
            // Keep current state when event is not valid for the state
        }
        BatteryNode updated = current.withTelemetry(solar, load, charge, next, health);
        nodes.put(id, updated);
        return updated;
    }

    public List<BatteryNode> sample(int limit) {
        int safe = Math.max(1, Math.min(limit, nodes.size()));
        return all().stream().limit(safe).toList();
    }

    public List<BatteryNode> zone(String zone) {
        return all().stream().filter(n -> n.zone().equalsIgnoreCase(zone)).toList();
    }
}
