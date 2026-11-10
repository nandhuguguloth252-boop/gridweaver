package com.gridweaver.controller;

import com.gridweaver.service.EventLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventLogService events;
    public EventController(EventLogService events){this.events=events;}
    @GetMapping public Object latest(@RequestParam(defaultValue="100") int limit){return events.latest(limit);}
}
