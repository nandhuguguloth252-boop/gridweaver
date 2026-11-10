package com.gridweaver.service;

import com.gridweaver.domain.BatteryNode;
import com.gridweaver.domain.GridEventEntity;
import com.gridweaver.repository.GridEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventLogService {
    private final GridEventRepository repository;
    public EventLogService(GridEventRepository repository) { this.repository = repository; }

    public void record(BatteryNode before, BatteryNode after, String reason) {
        if (before.state() == after.state()) return;
        repository.save(new GridEventEntity(before.id(), before.zone(), before.state().name(),
                after.state().name(), reason, after.loadKw(), after.solarKw()));
    }

    public List<GridEventEntity> latest(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 500))));
    }

    public long count() { return repository.count(); }
}
