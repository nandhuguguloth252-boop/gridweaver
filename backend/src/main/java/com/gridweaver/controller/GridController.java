package com.gridweaver.controller;

import com.gridweaver.service.GridBalanceService;
import com.gridweaver.service.GridSummaryService;
import com.gridweaver.websocket.GridWebSocketHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/grid")
public class GridController {
    private final GridSummaryService summary;
    private final GridBalanceService balance;
    private final GridWebSocketHandler webSocket;
    public GridController(GridSummaryService summary, GridBalanceService balance, GridWebSocketHandler webSocket) {
        this.summary=summary; this.balance=balance; this.webSocket=webSocket;
    }
    @GetMapping("/summary") public Map<String,Object> summary(){ return summary.summary(); }
    @GetMapping("/nodes") public Object nodes(@RequestParam(defaultValue="500") int limit){ return summary.mapNodes(limit); }
    @PostMapping("/balance") public Object balance(){ return balance.balance(); }
    @GetMapping("/ws-clients") public Map<String,Object> clients(){ return Map.of("clients", webSocket.clients()); }
}
