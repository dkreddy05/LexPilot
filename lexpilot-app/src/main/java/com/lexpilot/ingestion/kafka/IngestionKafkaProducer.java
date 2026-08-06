package com.lexpilot.ingestion.kafka;

import com.lexpilot.common.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link IngestionEvent} messages to the configured Kafka topic.
 * Uses the document ID as the Kafka key so all chunks for one document
 * land on the same partition (preserving ordering per document).
 */
@Component
public class IngestionKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(IngestionKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public IngestionKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, AppConfig appConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = appConfig.ingestion().kafkaTopic();
    }

    /**
     * Publish a single ingestion event (one per chunk).
     *
     * @param event the ingestion event to publish
     */
    public void publish(IngestionEvent event) {
        log.debug("Publishing {} event for doc={} chunkIndex={}",
                  event.eventType(), event.documentId(),
                  event.payload() != null ? event.payload().chunkIndex() : "n/a");

        kafkaTemplate.send(topic, event.documentId(), event);
    }
}
