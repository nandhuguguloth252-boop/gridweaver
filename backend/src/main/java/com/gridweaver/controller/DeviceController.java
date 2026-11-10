package com.gridweaver.controller;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.service.GridRegistryService;
import com.gridweaver.service.TelemetryIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class DeviceController {
    private final GridRegistryService registry;
    private final TelemetryIngestionService ingestion;

    public DeviceController(GridRegistryService registry, TelemetryIngestionService ingestion) {
        this.registry = registry;
        this.ingestion = ingestion;
    }

    @GetMapping("/devices")
    public Collection<BatteryNode> listDevices(@RequestParam(defaultValue = "100") int limit) {
        return registry.sample(limit);
    }

    @GetMapping("/devices/{id}")
    public ResponseEntity<BatteryNode> getDevice(@PathVariable String id) {
        BatteryNode node = registry.get(id);
        if (node == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(node);
    }

    @PostMapping("/devices")
    public ResponseEntity<BatteryNode> registerDevice(@RequestBody Map<String, Object> req) {
        String id = (String) req.getOrDefault("deviceId", req.getOrDefault("id", "BAT-" + System.currentTimeMillis()));
        String zone = (String) req.getOrDefault("zone", "NORTH");
        double lat = req.containsKey("latitude") ? ((Number) req.get("latitude")).doubleValue() : 26.9124;
        double lon = req.containsKey("longitude") ? ((Number) req.get("longitude")).doubleValue() : 75.7873;
        double capacity = req.containsKey("capacityKwh") ? ((Number) req.get("capacityKwh")).doubleValue() : 100.0;
        double charge = req.containsKey("batteryLevel") ? ((Number) req.get("batteryLevel")).doubleValue() : (capacity * 0.5);
        BatteryNode node = registry.registerOrGet(id, zone, lat, lon, capacity, charge);
        return ResponseEntity.ok(node);
    }

    @PostMapping("/telemetry")
    public ResponseEntity<BatteryNode> ingestTelemetry(@RequestBody TelemetryMessage message) {
        BatteryNode node = ingestion.ingest(message).join();
        if (node == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(node);
    }

    @PostMapping("/telemetry/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(@RequestBody List<TelemetryMessage> batch) {
        List<CompletableFuture<BatteryNode>> futures = batch.stream()
                .map(ingestion::ingest)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return ResponseEntity.ok(Map.of(
                "status", "PROCESSED",
                "count", batch.size(),
                "virtualThreads", true
        ));
    }
}
