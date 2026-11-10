package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GridBalanceService {
    private static final List<String> ZONES = List.of("NORTH", "SOUTH", "EAST", "WEST");
    private final GridRegistryService registry;
    private final Map<String, Double> lastTransfers = new LinkedHashMap<>();

    public GridBalanceService(GridRegistryService registry) {
        this.registry = registry;
    }

    public synchronized List<Map<String, Object>> balance() {
        record ZoneStats(String zone, double generation, double demand) {}

        Map<String, Double> generation = new LinkedHashMap<>();
        Map<String, Double> demand = new LinkedHashMap<>();
        for (String zone : ZONES) {
            generation.put(zone, registry.zone(zone).stream().mapToDouble(BatteryNode::solarKw).sum());
            demand.put(zone, registry.zone(zone).stream().mapToDouble(BatteryNode::loadKw).sum());
        }

        List<ZoneStats> receivers = ZONES.stream()
                .filter(zone -> demand.get(zone) > generation.get(zone))
                .map(zone -> new ZoneStats(zone, generation.get(zone), demand.get(zone)))
                .toList();
        List<ZoneStats> donors = ZONES.stream()
                .filter(zone -> generation.get(zone) > demand.get(zone))
                .map(zone -> new ZoneStats(zone, generation.get(zone), demand.get(zone)))
                .toList();

        List<Map<String, Object>> transfers = new ArrayList<>();
        Map<String, Double> remainingSupply = new HashMap<>();
        for (ZoneStats donor : donors) {
            remainingSupply.put(donor.zone(), donor.generation() - donor.demand());
        }

        for (ZoneStats receiver : receivers) {
            double need = receiver.demand() - receiver.generation();
            for (ZoneStats donor : donors) {
                double available = remainingSupply.getOrDefault(donor.zone(), 0.0);
                double amount = Math.min(need, Math.max(0, available));
                if (amount <= 0.01) continue;
                amount = round(amount);
                transfers.add(Map.of("from", donor.zone(), "to", receiver.zone(), "kw", amount));
                remainingSupply.put(donor.zone(), Math.max(0, available - amount));
                need -= amount;
                if (need <= 0.5) break;
            }
        }

        lastTransfers.clear();
        for (Map<String, Object> transfer : transfers) {
            String key = transfer.get("from") + "->" + transfer.get("to");
            lastTransfers.merge(key, ((Number) transfer.get("kw")).doubleValue(), Double::sum);
        }
        return List.copyOf(transfers);
    }

    public Map<String, Double> lastTransfers() {
        return Map.copyOf(lastTransfers);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
