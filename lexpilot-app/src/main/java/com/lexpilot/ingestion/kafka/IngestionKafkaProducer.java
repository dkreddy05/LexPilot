package com.lexpilot.ingestion.kafka;

import com.lexpilot.common.config.AppConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class IngestionKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppConfig appConfig;

    public IngestionKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, AppConfig appConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.appConfig = appConfig;
    }

    public void publish(String documentId) {
        // TODO: Publish ingestion event to Kafka topic
        throw new UnsupportedOperationException("IngestionKafkaProducer.publish() not yet implemented");
    }
}
