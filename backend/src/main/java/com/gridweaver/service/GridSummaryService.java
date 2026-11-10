package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.BatteryState;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GridSummaryService {
    private final GridRegistryService registry;
    private final TelemetryIngestionService ingestion;
    private final EventLogService events;
    private final GridBalanceService balance;

    public GridSummaryService(GridRegistryService registry, TelemetryIngestionService ingestion,
                              EventLogService events, GridBalanceService balance) {
        this.registry=registry; this.ingestion=ingestion; this.events=events; this.balance=balance;
    }

    public Map<String,Object> summary() {
        Collection<BatteryNode> nodes = registry.all();
        Map<BatteryState, Long> byState = new EnumMap<>(BatteryState.class);
        for (BatteryState s : BatteryState.values()) byState.put(s, 0L);
        double solar=0, load=0, capacity=0, charge=0, health=0;
        Map<String, double[]> zones = new LinkedHashMap<>();
        for (BatteryNode n : nodes) {
            byState.compute(n.state(), (k,v)->v+1);
            solar += n.solarKw(); load += n.loadKw(); capacity += n.capacityKwh(); charge += n.chargeKwh(); health += n.health();
            zones.computeIfAbsent(n.zone(), z->new double[3]);
            zones.get(n.zone())[0] += n.solarKw(); zones.get(n.zone())[1] += n.loadKw(); zones.get(n.zone())[2] += n.chargeKwh();
        }
        List<Map<String,Object>> zoneRows = zones.entrySet().stream().map(e -> {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("zone", e.getKey());
            row.put("solarKw", round(e.getValue()[0]));
            row.put("loadKw", round(e.getValue()[1]));
            row.put("batteryKwh", round(e.getValue()[2]));
            row.put("balanceKw", round(e.getValue()[0] - e.getValue()[1]));
            return row;
        }).toList();
        double resilience = Math.max(0, Math.min(100, 55 + (capacity == 0 ? 0 : charge/capacity*35) -
                (byState.get(BatteryState.FAULT) * 100.0 / Math.max(1,nodes.size())) * 40));
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("nodes", registry.size());
        out.put("solarKw", round(solar));
        out.put("loadKw", round(load));
        out.put("netKw", round(solar-load));
        out.put("batteryCapacityKwh", round(capacity));
        out.put("batteryChargeKwh", round(charge));
        out.put("avgHealth", round(health/Math.max(1,nodes.size())));
        out.put("resilience", round(resilience));
        out.put("states", byState);
        out.put("zones", zoneRows);
        out.put("events", events.count());
        out.put("virtualTasksSubmitted", ingestion.submitted());
        out.put("virtualTasksCompleted", ingestion.completed());
        out.put("transfers", balance.lastTransfers());
        return out;
    }
    public List<BatteryNode> mapNodes(int limit) { return registry.sample(limit); }
    private double round(double v){return Math.round(v*10.0)/10.0;}
}
