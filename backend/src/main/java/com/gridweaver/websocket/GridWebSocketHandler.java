package com.gridweaver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.service.TelemetryIngestionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GridWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final ObjectProvider<TelemetryIngestionService> ingestionProvider;
    private final int dashboardSampleLimit;

    public GridWebSocketHandler(
            ObjectProvider<TelemetryIngestionService> ingestionProvider,
            @Value("${grid.dashboard-sample-limit:800}") int dashboardSampleLimit) {
        this.ingestionProvider = ingestionProvider;
        this.dashboardSampleLimit = Math.max(1, dashboardSampleLimit);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(new ConcurrentWebSocketSessionDecorator(session, 10_000, 1024 * 1024));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.removeIf(existing -> existing.getId().equals(session.getId()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            if (payload == null || payload.isBlank()) return;
            TelemetryMessage telemetry = mapper.readValue(payload, TelemetryMessage.class);
            if (telemetry != null && telemetry.nodeId() != null) {
                TelemetryIngestionService ingestion = ingestionProvider.getIfAvailable();
                if (ingestion != null) {
                    ingestion.ingest(telemetry);
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed ping frames or invalid client messages
        }
    }

    public void broadcastNodeUpdate(BatteryNode node) {
        if (!isDashboardNode(node.id())) return;
        broadcast(Map.of("type", "node.update", "timestamp", Instant.now(), "node", node));
    }

    public void broadcastSummary(Object summary) {
        broadcast(Map.of("type", "grid.summary", "timestamp", Instant.now(), "payload", summary));
    }

    public int clients() {
        return sessions.size();
    }

    private boolean isDashboardNode(String id) {
        if (id == null || !id.startsWith("GW-")) return true; // Custom deviceIds (like BAT-*) are allowed
        try {
            int index = Integer.parseInt(id.substring(3));
            return index < dashboardSampleLimit;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private void broadcast(Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException | IllegalStateException ignored) {
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception ignored) {
            // A dashboard client error must never break telemetry ingestion
        }
    }
}
