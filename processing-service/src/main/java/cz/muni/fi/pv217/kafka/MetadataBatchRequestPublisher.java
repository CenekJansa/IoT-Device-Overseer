package cz.muni.fi.pv217.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.kafka.dto.MetadataBatchRequest;
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
 * Publisher for sending metadata batch requests to Kafka.
 * Publishes requests to the "metadata-batch-requests" topic.
 */
@ApplicationScoped
public class MetadataBatchRequestPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataBatchRequestPublisher.class);

    @Inject
    @Channel("metadata-batch-requests")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("MetadataBatchRequestPublisher is starting up and ready to publish to metadata-batch-requests topic.");
    }

    /**
     * Publishes a metadata batch request to Kafka.
     *
     * @param request The batch request to publish
     */
    @Timed("metadata_batch_request_publishing_time")
    @Counted("metadata_batch_request_publish_count")
    public void publish(MetadataBatchRequest request) {
        if (request == null) {
            LOG.warn("Attempted to publish null request, skipping");
            return;
        }

        if (request.batchId() == null || request.batchId().isEmpty()) {
            LOG.error("Cannot publish request with null or empty batch ID");
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(request);
            emitter.send(message);
            LOG.debug("Successfully published metadata batch request for batch ID: {} with {} device IDs",
                     request.batchId(), request.deviceIds() != null ? request.deviceIds().size() : 0);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize metadata batch request for batch ID: {}. Error: {}",
                     request.batchId(), e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Error publishing metadata batch request for batch ID: {}. Error: {}",
                     request.batchId(), e.getMessage(), e);
        }
    }
}

