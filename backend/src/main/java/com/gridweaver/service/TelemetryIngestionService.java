package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.websocket.GridWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TelemetryIngestionService {
    private final ExecutorService executor;
    private final GridRegistryService registry;
    private final EventLogService events;
    private final GridWebSocketHandler webSocket;
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();

    public TelemetryIngestionService(ExecutorService executor, GridRegistryService registry,
                                     EventLogService events, GridWebSocketHandler webSocket) {
        this.executor = executor; this.registry = registry; this.events = events; this.webSocket = webSocket;
    }

    public CompletableFuture<BatteryNode> ingest(TelemetryMessage message) {
        submitted.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            BatteryNode before = registry.get(message.nodeId());
            BatteryNode after = registry.updateTelemetry(message.nodeId(), message.solarKw(),
                    message.loadKw(), message.chargeKwh(), message.effectiveHealth());
            completed.incrementAndGet();
            if (after != null) {
                if (before != null && before.state() != after.state()) {
                    events.record(before, after, "telemetry");
                }
                webSocket.broadcastNodeUpdate(after);
            }
            return after;
        }, executor);
    }

    public long submitted() { return submitted.get(); }
    public long completed() { return completed.get(); }
}
