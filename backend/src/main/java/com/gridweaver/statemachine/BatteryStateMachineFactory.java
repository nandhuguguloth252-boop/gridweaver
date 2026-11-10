package com.gridweaver.statemachine;

import com.gridweaver.domain.BatteryEvent;
import com.gridweaver.domain.BatteryState;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class BatteryStateMachineFactory {
    public StateMachine<BatteryState, BatteryEvent> create(BatteryState initial) {
        try {
            var builder = StateMachineBuilder.<BatteryState, BatteryEvent>builder();
            builder.configureConfiguration().withConfiguration().autoStartup(true);
            builder.configureStates().withStates()
                    .initial(initial)
                    .states(EnumSet.allOf(BatteryState.class));
            builder.configureTransitions()
                    .withExternal().source(BatteryState.IDLE).target(BatteryState.CHARGING).event(BatteryEvent.SOLAR_SURPLUS).and()
                    .withExternal().source(BatteryState.IDLE).target(BatteryState.DISCHARGING).event(BatteryEvent.GRID_LOAD_HIGH).and()
                    .withExternal().source(BatteryState.IDLE).target(BatteryState.DISCHARGING).event(BatteryEvent.DEMAND_SURGE).and()
                    .withExternal().source(BatteryState.CHARGING).target(BatteryState.DISCHARGING).event(BatteryEvent.DEMAND_SURGE).and()
                    .withExternal().source(BatteryState.CHARGING).target(BatteryState.DISCHARGING).event(BatteryEvent.GRID_LOAD_HIGH).and()
                    .withExternal().source(BatteryState.CHARGING).target(BatteryState.IDLE).event(BatteryEvent.GRID_LOAD_NORMAL).and()
                    .withExternal().source(BatteryState.DISCHARGING).target(BatteryState.CHARGING).event(BatteryEvent.SOLAR_SURPLUS).and()
                    .withExternal().source(BatteryState.DISCHARGING).target(BatteryState.IDLE).event(BatteryEvent.GRID_LOAD_NORMAL).and()
                    .withExternal().source(BatteryState.IDLE).target(BatteryState.FAULT).event(BatteryEvent.FAULT_DETECTED).and()
                    .withExternal().source(BatteryState.CHARGING).target(BatteryState.FAULT).event(BatteryEvent.FAULT_DETECTED).and()
                    .withExternal().source(BatteryState.DISCHARGING).target(BatteryState.FAULT).event(BatteryEvent.FAULT_DETECTED).and()
                    .withExternal().source(BatteryState.FAULT).target(BatteryState.IDLE).event(BatteryEvent.FAULT_RECOVERED).and();
            var machine = builder.build();
            machine.start();
            return machine;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build battery state machine", ex);
        }
    }

    public BatteryState apply(StateMachine<BatteryState, BatteryEvent> machine, BatteryEvent event) {
        machine.sendEvent(MessageBuilder.withPayload(event).build());
        return machine.getState().getId();
    }
}
