package cz.muni.fi.pv217.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.kafka.dto.MetadataBatchResponse;
import cz.muni.fi.pv217.service.EventDataEnricher;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for metadata batch responses from Kafka.
 * Consumes messages from the "metadata-batch-responses" topic and delegates to the enricher.
 */
@ApplicationScoped
public class MetadataBatchResponseListener {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataBatchResponseListener.class);

    @Inject
    EventDataEnricher enricher;

    @Inject
    ObjectMapper objectMapper;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("MetadataBatchResponseListener is starting up and ready to consume from metadata-batch-responses topic.");
    }

    /**
     * Consumes metadata batch responses from Kafka and passes them to the enricher.
     *
     * @param message The JSON message from Kafka
     */
    @Timed("metadata_batch_response_processing_time")
    @Counted("metadata_batch_response_count")
    @Incoming("metadata-batch-responses")
    public void onMetadataBatchResponse(String message) {
        if (message == null || message.isEmpty()) {
            LOG.debug("Received empty message, skipping processing");
            return;
        }

        LOG.debug("Received metadata batch response from Kafka");

        try {
            // todo: toto je shit
            MetadataBatchResponse response = objectMapper.readValue(message, MetadataBatchResponse.class);
            
            if (response.batchId() == null || response.batchId().isEmpty()) {
                LOG.error("Received response with null or empty batch ID, skipping");
                return;
            }

            LOG.info("Processing metadata batch response for batch ID: {} with {} metadata entries",
                     response.batchId(), response.metadata() != null ? response.metadata().size() : 0);

            // Delegate to enricher to complete the enrichment process
            enricher.completeEnrichment(response);

        } catch (JsonProcessingException e) {
            LOG.error("Failed to deserialize metadata batch response: {}. Error: {}", message, e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Error processing metadata batch response: {}. Error: {}", message, e.getMessage(), e);
        }
    }
}

