package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.BatteryState;
import com.gridweaver.statemachine.BatteryStateMachineFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Regional Balancing Service Unit Tests")
class GridBalanceServiceTest {

    private GridRegistryService registry;
    private GridBalanceService balanceService;

    @BeforeEach
    void setUp() {
        BatteryStateMachineFactory factory = new BatteryStateMachineFactory();
        registry = new GridRegistryService(factory, 0); // start with empty seed for custom tests
        balanceService = new GridBalanceService(registry);
    }

    @Test
    @DisplayName("Balance transfers power from surplus zone to deficit zone")
    void testSurplusToDeficitBalancing() {
        // NORTH: Generation 100 kW, Demand 40 kW (Surplus = +60 kW)
        registry.register(new BatteryNode("GW-N1", "NORTH", 26.9, 75.8, 100, 50,
                100.0, 40.0, BatteryState.IDLE, 95.0, 0, Instant.now()));

        // SOUTH: Generation 20 kW, Demand 70 kW (Deficit = -50 kW)
        registry.register(new BatteryNode("GW-S1", "SOUTH", 26.8, 75.7, 100, 50,
                20.0, 70.0, BatteryState.IDLE, 95.0, 0, Instant.now()));

        List<Map<String, Object>> transfers = balanceService.balance();
        assertFalse(transfers.isEmpty(), "Should produce balancing transfers");

        Map<String, Object> transfer = transfers.get(0);
        assertEquals("NORTH", transfer.get("from"));
        assertEquals("SOUTH", transfer.get("to"));
        double kw = ((Number) transfer.get("kw")).doubleValue();
        assertEquals(50.0, kw, 0.1, "Should transfer 50 kW to satisfy deficit");

        Map<String, Double> lastTransfers = balanceService.lastTransfers();
        assertTrue(lastTransfers.containsKey("NORTH->SOUTH"));
        assertEquals(50.0, lastTransfers.get("NORTH->SOUTH"), 0.1);
    }

    @Test
    @DisplayName("Balance does not exceed donor zone surplus capacity")
    void testDonorCapacityLimit() {
        // EAST: Generation 50 kW, Demand 30 kW (Surplus = +20 kW)
        registry.register(new BatteryNode("GW-E1", "EAST", 26.9, 75.9, 100, 50,
                50.0, 30.0, BatteryState.IDLE, 95.0, 0, Instant.now()));

        // WEST: Generation 10 kW, Demand 90 kW (Deficit = -80 kW)
        registry.register(new BatteryNode("GW-W1", "WEST", 26.7, 75.6, 100, 50,
                10.0, 90.0, BatteryState.IDLE, 95.0, 0, Instant.now()));

        List<Map<String, Object>> transfers = balanceService.balance();
        assertFalse(transfers.isEmpty());

        double totalTransferred = transfers.stream()
                .filter(t -> "EAST".equals(t.get("from")))
                .mapToDouble(t -> ((Number) t.get("kw")).doubleValue())
                .sum();

        assertTrue(totalTransferred <= 20.01, "Transferred amount must not exceed surplus (+20 kW)");
    }

    @Test
    @DisplayName("No transfers when all zones have surplus or zero demand")
    void testZeroDeficitProducesNoTransfers() {
        registry.register(new BatteryNode("GW-1", "NORTH", 26.9, 75.8, 100, 50,
                100.0, 20.0, BatteryState.IDLE, 95.0, 0, Instant.now()));
        registry.register(new BatteryNode("GW-2", "SOUTH", 26.8, 75.7, 100, 50,
                80.0, 30.0, BatteryState.IDLE, 95.0, 0, Instant.now()));

        List<Map<String, Object>> transfers = balanceService.balance();
        assertTrue(transfers.isEmpty(), "No transfers needed when no zone has deficit");
    }
}
