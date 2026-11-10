package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.kafka.TelemetryKafkaAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SimulationService {
    private final GridRegistryService registry;
    private final TelemetryIngestionService ingestion;
    private final GridBalanceService balanceService;
    private final ObjectProvider<TelemetryKafkaAdapter> kafkaAdapter;
    private final boolean kafkaEnabled;
    private final String kafkaTopic;

    public SimulationService(
            GridRegistryService registry,
            TelemetryIngestionService ingestion,
            GridBalanceService balanceService,
            ObjectProvider<TelemetryKafkaAdapter> kafkaAdapter,
            @Value("${grid.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${grid.kafka.topic:grid.telemetry}") String kafkaTopic) {
        this.registry = registry;
        this.ingestion = ingestion;
        this.balanceService = balanceService;
        this.kafkaAdapter = kafkaAdapter;
        this.kafkaEnabled = kafkaEnabled;
        this.kafkaTopic = kafkaTopic;
    }

    public Map<String, Object> storm(int requested) {
        int count = Math.min(Math.max(1, requested), registry.size());
        List<BatteryNode> nodes = registry.sample(count);
        Instant start = Instant.now();

        List<CompletableFuture<BatteryNode>> futures = new ArrayList<>(nodes.size());
        for (BatteryNode n : nodes) {
            futures.add(ingestion.ingest(new TelemetryMessage(
                    n.id(),
                    n.solarKw() * 0.40,
                    n.loadKw() * (1.35 + ThreadLocalRandom.current().nextDouble(0.40)),
                    Math.max(0, n.chargeKwh() - 2),
                    Math.max(10, n.health() - 0.3),
                    Instant.now())));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long ms = Math.max(1, Duration.between(start, Instant.now()).toMillis());
        return Map.of(
                "mode", "STORM",
                "requestedNodes", requested,
                "processedNodes", count,
                "registeredNodes", registry.size(),
                "elapsedMs", ms,
                "nodesPerSecond", round(count * 1000.0 / ms),
                "balanceActions", balanceService.balance().size(),
                "virtualThreads", true
        );
    }

    public Map<String, Object> benchmark(int requested) {
        int count = Math.min(Math.max(1, requested), registry.size());
        List<BatteryNode> nodes = registry.sample(count);
        Instant start = Instant.now();
        List<CompletableFuture<?>> futures = new ArrayList<>(count);
        for (BatteryNode n : nodes) {
            futures.add(ingestion.ingest(new TelemetryMessage(
                    n.id(), 55, 45, n.chargeKwh(), n.health(), Instant.now())));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long ms = Math.max(1, Duration.between(start, Instant.now()).toMillis());
        return Map.of(
                "mode", "VIRTUAL_THREAD_BENCHMARK",
                "requestedTasks", requested,
                "tasks", count,
                "registeredNodes", registry.size(),
                "elapsedMs", ms,
                "tasksPerSecond", round(count * 1000.0 / ms),
                "virtualThreads", true
        );
    }

    public Map<String, Object> kafkaPulse(int requested) {
        if (!kafkaEnabled) {
            return Map.of("enabled", false, "message", "Kafka is disabled. Start GridWeaver with GRID_KAFKA_ENABLED=true.");
        }
        TelemetryKafkaAdapter adapter = kafkaAdapter.getIfAvailable();
        if (adapter == null) {
            return Map.of("enabled", false, "message", "Kafka adapter is not available.");
        }

        int count = Math.min(Math.max(1, requested), registry.size());
        int published = 0;
        for (BatteryNode n : registry.sample(count)) {
            adapter.publish(kafkaTopic, new TelemetryMessage(
                    n.id(),
                    Math.max(0, n.solarKw() + ThreadLocalRandom.current().nextDouble(-5, 8)),
                    Math.max(3, n.loadKw() + ThreadLocalRandom.current().nextDouble(-4, 6)),
                    n.chargeKwh(), n.health(), Instant.now()));
            published++;
        }
        return Map.of(
                "mode", "KAFKA_PULSE",
                "topic", kafkaTopic,
                "published", published,
                "registeredNodes", registry.size(),
                "broker", "Kafka"
        );
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
