package com.gridweaver.controller;

import com.gridweaver.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {
    private final SimulationService simulation;

    public SimulationController(SimulationService simulation) {
        this.simulation = simulation;
    }

    @PostMapping("/storm")
    public Map<String, Object> storm(@RequestParam(defaultValue = "10000") int nodes) {
        return simulation.storm(nodes);
    }

    @PostMapping("/benchmark")
    public Map<String, Object> benchmark(@RequestParam(defaultValue = "50000") int nodes) {
        return simulation.benchmark(nodes);
    }

    @PostMapping("/kafka-pulse")
    public Map<String, Object> kafkaPulse(@RequestParam(defaultValue = "1000") int nodes) {
        return simulation.kafkaPulse(nodes);
    }
}
