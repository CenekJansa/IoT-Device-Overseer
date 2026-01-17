package cz.muni.fi.pv217.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.entity.to.InputEventTo;
import cz.muni.fi.pv217.service.ProcessingOrchestrator;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for listening to the "raw-telemetry" topic and processing incoming events in batches.
 * It consumes batches of messages from Kafka, deserializes them, and passes them to the ProcessingOrchestrator.
 * Batch processing helps reduce the load on the DeviceService by making batched API calls.
 */
@ApplicationScoped
public class InputEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(InputEventListener.class);

    @Inject
    ProcessingOrchestrator orchestrator;

    @Inject
    ObjectMapper objectMapper;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("InputEventListener is starting up and ready to consume batches from raw-telemetry topic.");
    }

    /**
     * Consumes batches of messages from the "sensor-ingest" channel (raw-telemetry topic).
     * Messages are expected to be JSON strings that can be deserialized to InputEventTo.
     * Processing in batches allows for more efficient calls to external services like DeviceService.
     *
     * @param messages The batch of JSON messages from Kafka
     */
    @Timed("input_event_batch_processing_time")
    @Counted("input_event_batch_count")
    @Incoming("sensor-ingest")
    public void onInputEventBatch(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            LOG.debug("Received empty batch, skipping processing");
            return;
        }

        LOG.info("Received batch of {} messages from Kafka", messages.size());

        List<InputEventTo> events = new ArrayList<>();
        List<String> failedMessages = new ArrayList<>();

        // Deserialize all messages in the batch
        for (String message : messages) {
            try {
                InputEventTo event = objectMapper.readValue(message, InputEventTo.class);
                events.add(event);
            } catch (JsonProcessingException e) {
                LOG.error("Failed to deserialize message: {}. Error: {}", message, e.getMessage(), e);
                failedMessages.add(message);
            } catch (Exception e) {
                LOG.error("Error deserializing message: {}. Error: {}", message, e.getMessage(), e);
                failedMessages.add(message);
            }
        }

        // Process the batch of successfully deserialized events
        if (!events.isEmpty()) {
            try {
                orchestrator.processBatch(events);
                LOG.info("Successfully processed batch of {} events", events.size());
            } catch (Exception e) {
                LOG.error("Error processing batch of {} events. Error: {}", events.size(), e.getMessage(), e);
            }
        }

        if (!failedMessages.isEmpty()) {
            LOG.warn("Failed to deserialize {} out of {} messages in batch",
                    failedMessages.size(), messages.size());
        }
    }
}
