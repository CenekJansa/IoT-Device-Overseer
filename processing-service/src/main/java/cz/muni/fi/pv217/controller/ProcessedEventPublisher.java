package cz.muni.fi.pv217.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.entity.to.ProcessedEventTo;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for publishing processed events to the "processing-data" Kafka topic.
 * It serializes ProcessedEventTo objects to JSON and sends them to the Kafka stream.
 */
@ApplicationScoped
public class ProcessedEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessedEventPublisher.class);

    @Inject
    @Channel("processed-data-stream")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("ProcessedEventPublisher is starting up and ready to publish to processing-data topic.");
    }

    /**
     * Publishes a processed event to the Kafka topic.
     *
     * @param event The processed event to publish
     */
    @Timed("processed_event_publishing_time")
    @Counted("processed_event_publish_count")
    public void publish(ProcessedEventTo event) {
        if (event == null) {
            LOG.warn("Attempted to publish null event, skipping");
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(event);
            emitter.send(message);
            LOG.debug("Successfully published processed event for device: {} at timestamp: {}",
                     event.getDeviceId(), event.getTimestamp());
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize processed event for device: {}. Error: {}",
                     event.getDeviceId(), e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Error publishing processed event for device: {}. Error: {}",
                     event.getDeviceId(), e.getMessage(), e);
        }
    }
}
