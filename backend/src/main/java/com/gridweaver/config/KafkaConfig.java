package com.gridweaver.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "grid.kafka.enabled", havingValue = "true")
public class KafkaConfig {
    @Bean
    public NewTopic gridTelemetryTopic(@Value("${grid.kafka.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(6).replicas(1).build();
    }
}
