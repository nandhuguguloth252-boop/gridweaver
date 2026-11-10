package com.gridweaver.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridweaver.domain.TelemetryMessage;
import com.gridweaver.service.TelemetryIngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "grid.kafka.enabled", havingValue = "true")
public class TelemetryKafkaAdapter {
    private final KafkaTemplate<String, String> kafka;
    private final TelemetryIngestionService ingestion;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public TelemetryKafkaAdapter(KafkaTemplate<String, String> kafka, TelemetryIngestionService ingestion) {
        this.kafka=kafka; this.ingestion=ingestion;
    }

    public void publish(String topic, TelemetryMessage message) {
        try { kafka.send(topic, message.nodeId(), mapper.writeValueAsString(message)); }
        catch (Exception ignored) {}
    }

    @KafkaListener(topics = "${grid.kafka.topic:grid.telemetry}")
    public void consume(String payload) {
        try { ingestion.ingest(mapper.readValue(payload, TelemetryMessage.class)); }
        catch (Exception ignored) {}
    }
}
