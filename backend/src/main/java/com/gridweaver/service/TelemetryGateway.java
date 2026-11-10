package com.gridweaver.service;

import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.kafka.TelemetryKafkaAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TelemetryGateway {
    private final TelemetryIngestionService ingestion;
    private final ObjectProvider<TelemetryKafkaAdapter> kafkaAdapter;
    private final boolean kafkaEnabled;
    private final String topic;

    public TelemetryGateway(
            TelemetryIngestionService ingestion,
            ObjectProvider<TelemetryKafkaAdapter> kafkaAdapter,
            @Value("${grid.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${grid.kafka.topic:grid.telemetry}") String topic) {
        this.ingestion = ingestion;
        this.kafkaAdapter = kafkaAdapter;
        this.kafkaEnabled = kafkaEnabled;
        this.topic = topic;
    }

    public void submit(TelemetryMessage message) {
        TelemetryKafkaAdapter adapter = kafkaAdapter.getIfAvailable();
        if (kafkaEnabled && adapter != null) {
            adapter.publish(topic, message);
            return;
        }
        ingestion.ingest(message);
    }
}
