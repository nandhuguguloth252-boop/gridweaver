package com.gridweaver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.domain.TelemetryMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "grid.kafka.enabled=false",
        "grid.simulator.enabled=false",
        "grid.node-count=500"
})
@AutoConfigureMockMvc
@DisplayName("GridWeaver REST Controllers Integration Tests")
class GridControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/grid/summary returns comprehensive grid stats")
    void testGetSummary() throws Exception {
        mockMvc.perform(get("/api/grid/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", greaterThanOrEqualTo(500)))
                .andExpect(jsonPath("$.resilience", notNullValue()))
                .andExpect(jsonPath("$.states", notNullValue()))
                .andExpect(jsonPath("$.zones", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.transfers", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/grid/nodes returns sampled digital-twin nodes")
    void testGetNodes() throws Exception {
        mockMvc.perform(get("/api/grid/nodes?limit=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)))
                .andExpect(jsonPath("$[0].id", startsWith("GW-")))
                .andExpect(jsonPath("$[0].state", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/grid/balance triggers regional power balancing")
    void testPostBalance() throws Exception {
        mockMvc.perform(post("/api/grid/balance"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/events returns event audit log")
    void testGetEvents() throws Exception {
        mockMvc.perform(get("/api/events?limit=10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/telemetry ingests single device telemetry via Virtual Threads")
    void testIngestTelemetry() throws Exception {
        TelemetryMessage msg = new TelemetryMessage("BAT-REST-1", 50.0, 15.0, 80.0, 95.0, Instant.now());
        String json = objectMapper.writeValueAsString(msg);

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BAT-REST-1")))
                .andExpect(jsonPath("$.solarKw", is(50.0)))
                .andExpect(jsonPath("$.loadKw", is(15.0)));
    }

    @Test
    @DisplayName("POST /api/telemetry/batch ingests batch telemetry")
    void testIngestBatchTelemetry() throws Exception {
        List<TelemetryMessage> batch = List.of(
                new TelemetryMessage("BAT-BATCH-1", 30.0, 20.0, 60.0, 95.0, Instant.now()),
                new TelemetryMessage("BAT-BATCH-2", 10.0, 65.0, 80.0, 95.0, Instant.now())
        );
        String json = objectMapper.writeValueAsString(batch);

        mockMvc.perform(post("/api/telemetry/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")))
                .andExpect(jsonPath("$.count", is(2)));
    }

    @Test
    @DisplayName("POST /api/devices registers new IoT device")
    void testRegisterDevice() throws Exception {
        Map<String, Object> req = Map.of(
                "deviceId", "BAT-NEW-999",
                "zone", "EAST",
                "latitude", 26.915,
                "longitude", 75.789,
                "capacityKwh", 150.0,
                "batteryLevel", 90.0
        );

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BAT-NEW-999")))
                .andExpect(jsonPath("$.zone", is("EAST")))
                .andExpect(jsonPath("$.capacityKwh", is(150.0)));

        mockMvc.perform(get("/api/devices/BAT-NEW-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BAT-NEW-999")));
    }

    @Test
    @DisplayName("POST /api/simulation/benchmark runs Virtual Thread benchmark")
    void testVirtualThreadBenchmark() throws Exception {
        mockMvc.perform(post("/api/simulation/benchmark?nodes=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("VIRTUAL_THREAD_BENCHMARK")))
                .andExpect(jsonPath("$.virtualThreads", is(true)))
                .andExpect(jsonPath("$.tasksPerSecond", greaterThan(0.0)));
    }

    @Test
    @DisplayName("POST /api/simulation/storm runs storm scenario")
    void testSimulationStorm() throws Exception {
        mockMvc.perform(post("/api/simulation/storm?nodes=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("STORM")))
                .andExpect(jsonPath("$.virtualThreads", is(true)));
    }
}
