package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.BatteryState;
import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.statemachine.BatteryStateMachineFactory;
import com.gridweaver.websocket.GridWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Telemetry Ingestion Service & Virtual Threads Unit Tests")
class TelemetryIngestionServiceTest {

    @Mock
    private EventLogService eventLogService;

    @Mock
    private GridWebSocketHandler webSocketHandler;

    private ExecutorService virtualExecutor;
    private GridRegistryService registry;
    private TelemetryIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        BatteryStateMachineFactory factory = new BatteryStateMachineFactory();
        registry = new GridRegistryService(factory, 100);
        registry.seed();
        ingestionService = new TelemetryIngestionService(virtualExecutor, registry, eventLogService, webSocketHandler);
    }

    @Test
    @DisplayName("Virtual Thread ingestion processes telemetry message asynchronously")
    void testIngestTelemetry() throws Exception {
        BatteryNode node = registry.get("GW-00000");
        assertNotNull(node);

        TelemetryMessage msg = new TelemetryMessage(node.id(), 80.0, 10.0, 50.0, 95.0, Instant.now());
        CompletableFuture<BatteryNode> future = ingestionService.ingest(msg);

        BatteryNode result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(80.0, result.solarKw());
        assertEquals(10.0, result.loadKw());
        assertEquals(BatteryState.CHARGING, result.state());

        assertTrue(ingestionService.submitted() >= 1);
        assertTrue(ingestionService.completed() >= 1);
        verify(webSocketHandler, atLeastOnce()).broadcastNodeUpdate(any(BatteryNode.class));
    }

    @Test
    @DisplayName("Concurrent ingestion of 1,000 virtual-thread tasks executes rapidly")
    void testHighConcurrencyVirtualThreads() throws Exception {
        int taskCount = 1000;
        List<CompletableFuture<BatteryNode>> futures = new ArrayList<>(taskCount);

        long start = System.currentTimeMillis();
        for (int i = 0; i < taskCount; i++) {
            String nodeId = "GW-" + String.format("%05d", i % 100);
            TelemetryMessage msg = new TelemetryMessage(nodeId, 40.0, 20.0, 50.0, 95.0, Instant.now());
            futures.add(ingestionService.ingest(msg));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(taskCount, ingestionService.completed());
        assertTrue(elapsed < 5000, "1,000 tasks on virtual threads must complete in < 5s, was " + elapsed + "ms");
    }
}
