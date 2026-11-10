package com.gridweaver.statemachine;

import com.gridweaver.domain.BatteryEvent;
import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.BatteryState;
import com.gridweaver.service.GridRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.StateMachine;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Battery State Machine & Rules Unit Tests")
class BatteryStateMachineTest {

    private BatteryStateMachineFactory factory;
    private GridRegistryService registry;

    @BeforeEach
    void setUp() {
        factory = new BatteryStateMachineFactory();
        registry = new GridRegistryService(factory, 100);
    }

    @Test
    @DisplayName("Test 1: Normal Grid Load (50%) & Normal Solar -> IDLE")
    void testIdleStateUnderNormalConditions() {
        StateMachine<BatteryState, BatteryEvent> sm = factory.create(BatteryState.IDLE);
        assertEquals(BatteryState.IDLE, sm.getState().getId());

        BatteryState next = factory.apply(sm, BatteryEvent.GRID_LOAD_NORMAL);
        assertEquals(BatteryState.IDLE, next);

        // Through registry telemetry
        BatteryNode node = registry.registerOrGet("BAT-TEST-1", "NORTH", 26.9, 75.7, 100, 70);
        BatteryNode updated = registry.updateTelemetry("BAT-TEST-1", 20.0, 20.0, 70.0, 95.0);
        assertEquals(BatteryState.IDLE, updated.state());
    }

    @Test
    @DisplayName("Test 2: Solar Generation > Demand -> CHARGING")
    void testSolarSurplusTransitionsToCharging() {
        StateMachine<BatteryState, BatteryEvent> sm = factory.create(BatteryState.IDLE);
        BatteryState next = factory.apply(sm, BatteryEvent.SOLAR_SURPLUS);
        assertEquals(BatteryState.CHARGING, next);

        // Through registry telemetry: solar 50kW, load 20kW
        BatteryNode node = registry.registerOrGet("BAT-TEST-2", "EAST", 26.9, 75.7, 100, 50);
        BatteryNode updated = registry.updateTelemetry("BAT-TEST-2", 50.0, 20.0, 55.0, 95.0);
        assertEquals(BatteryState.CHARGING, updated.state());
    }

    @Test
    @DisplayName("Test 3: Grid Load High (load > 42kW) -> DISCHARGING")
    void testHighGridLoadTransitionsToDischarging() {
        StateMachine<BatteryState, BatteryEvent> sm = factory.create(BatteryState.IDLE);
        BatteryState next = factory.apply(sm, BatteryEvent.GRID_LOAD_HIGH);
        assertEquals(BatteryState.DISCHARGING, next);

        // Through registry telemetry: load 48kW > 42kW
        BatteryNode node = registry.registerOrGet("BAT-TEST-3", "WEST", 26.9, 75.7, 100, 80);
        BatteryNode updated = registry.updateTelemetry("BAT-TEST-3", 10.0, 48.0, 75.0, 95.0);
        assertEquals(BatteryState.DISCHARGING, updated.state());
    }

    @Test
    @DisplayName("Test 4: Demand Surge (load >= 60kW) from IDLE -> DISCHARGING")
    void testDemandSurgeTransitionsToDischarging() {
        StateMachine<BatteryState, BatteryEvent> sm = factory.create(BatteryState.IDLE);
        BatteryState next = factory.apply(sm, BatteryEvent.DEMAND_SURGE);
        assertEquals(BatteryState.DISCHARGING, next);

        // Through registry telemetry: load 75kW
        BatteryNode node = registry.registerOrGet("BAT-TEST-4", "SOUTH", 26.9, 75.7, 100, 80);
        BatteryNode updated = registry.updateTelemetry("BAT-TEST-4", 10.0, 75.0, 70.0, 95.0);
        assertEquals(BatteryState.DISCHARGING, updated.state());
    }

    @Test
    @DisplayName("Test 5: Overheat (temp > 60°C or health < 20) -> FAULT")
    void testOverheatTransitionsToFault() {
        // From IDLE
        StateMachine<BatteryState, BatteryEvent> smIdle = factory.create(BatteryState.IDLE);
        assertEquals(BatteryState.FAULT, factory.apply(smIdle, BatteryEvent.FAULT_DETECTED));

        // From CHARGING
        StateMachine<BatteryState, BatteryEvent> smCharging = factory.create(BatteryState.CHARGING);
        assertEquals(BatteryState.FAULT, factory.apply(smCharging, BatteryEvent.FAULT_DETECTED));

        // From DISCHARGING
        StateMachine<BatteryState, BatteryEvent> smDischarging = factory.create(BatteryState.DISCHARGING);
        assertEquals(BatteryState.FAULT, factory.apply(smDischarging, BatteryEvent.FAULT_DETECTED));

        // Through registry telemetry: health 10 (< 20)
        BatteryNode node = registry.registerOrGet("BAT-TEST-5", "NORTH", 26.9, 75.7, 100, 80);
        BatteryNode updated = registry.updateTelemetry("BAT-TEST-5", 10.0, 20.0, 80.0, 10.0);
        assertEquals(BatteryState.FAULT, updated.state());
    }

    @Test
    @DisplayName("Test 6: FAULT + Recovery Event (health >= 25) -> IDLE")
    void testFaultRecoveryTransitionsToIdle() {
        StateMachine<BatteryState, BatteryEvent> sm = factory.create(BatteryState.FAULT);
        BatteryState next = factory.apply(sm, BatteryEvent.FAULT_RECOVERED);
        assertEquals(BatteryState.IDLE, next);

        // Through registry telemetry: starts at FAULT, then recovers with health = 90
        registry.register(new BatteryNode("BAT-TEST-6", "NORTH", 26.9, 75.7, 100, 80,
                10, 20, BatteryState.FAULT, 10.0, 0, Instant.now()));

        BatteryNode recovered = registry.updateTelemetry("BAT-TEST-6", 20.0, 20.0, 80.0, 90.0);
        assertEquals(BatteryState.IDLE, recovered.state());
    }

    @Test
    @DisplayName("Test 7: Stabilized Grid (Normal Load) from CHARGING -> IDLE")
    void testChargingToIdleOnGridLoadNormal() {
        StateMachine<BatteryState, BatteryEvent> sm = factory.create(BatteryState.CHARGING);
        BatteryState next = factory.apply(sm, BatteryEvent.GRID_LOAD_NORMAL);
        assertEquals(BatteryState.IDLE, next);
    }

    @Test
    @DisplayName("Test 8: Stabilized Grid (Normal Load) from DISCHARGING -> IDLE")
    void testDischargingToIdleOnGridLoadNormal() {
        StateMachine<BatteryState, BatteryEvent> sm = factory.create(BatteryState.DISCHARGING);
        BatteryState next = factory.apply(sm, BatteryEvent.GRID_LOAD_NORMAL);
        assertEquals(BatteryState.IDLE, next);
    }
}
