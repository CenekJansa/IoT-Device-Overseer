package cz.muni.fi.pv217.service;

import cz.muni.fi.pv217.cache.BatchCacheService;
import cz.muni.fi.pv217.entity.to.DeviceMetadataResTo;
import cz.muni.fi.pv217.controller.ProcessedEventPublisher;
import cz.muni.fi.pv217.entity.mappers.ProcessedEventToMapper;
import cz.muni.fi.pv217.entity.to.ProcessedEventTo;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import cz.muni.fi.pv217.entity.vo.ProcessedEventVo;
import cz.muni.fi.pv217.entity.mappers.ProcessedEventVoMapper;
import cz.muni.fi.pv217.kafka.MetadataBatchRequestPublisher;
import cz.muni.fi.pv217.kafka.dto.MetadataBatchRequest;
import cz.muni.fi.pv217.kafka.dto.MetadataBatchResponse;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EventDataEnricher {

    private static final Logger LOG = LoggerFactory.getLogger(EventDataEnricher.class);

    private final BatchCacheService cacheService;
    private final MetadataBatchRequestPublisher requestPublisher;
    private final MetricEvaluator processor;
    private final ProcessedEventPublisher eventPublisher;

    public EventDataEnricher(BatchCacheService cacheService,
                             MetadataBatchRequestPublisher requestPublisher,
                             MetricEvaluator processor,
                             ProcessedEventPublisher eventPublisher) {
        this.cacheService = cacheService;
        this.requestPublisher = requestPublisher;
        this.processor = processor;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Initiates batch enrichment by storing events in Redis and publishing a metadata request to Kafka.
     * This is now an async operation - the actual enrichment will be completed when the response arrives.
     *
     * @param events The list of input events to enrich
     */
    public void enrichBatch(List<InputEventVo> events) {
        if (events == null || events.isEmpty()) {
            LOG.warn("Received null or empty batch for enrichment");
            return;
        }

        LOG.info("Starting async enrichment for batch of {} events", events.size());

        // Generate unique batch ID
        String batchId = UUID.randomUUID().toString();

        // Store events in Redis cache
        cacheService.storeBatch(batchId, events);

        // Extract unique device IDs from the batch
        Set<UUID> uniqueDeviceIds = events.stream()
            .map(InputEventVo::getDeviceId)
            .collect(Collectors.toSet());

        LOG.info("Publishing metadata request for batch {} with {} unique devices",
                 batchId, uniqueDeviceIds.size());

        // Create and publish metadata request to Kafka
        MetadataBatchRequest request = new MetadataBatchRequest(
            batchId,
            new ArrayList<>(uniqueDeviceIds)
        );
        requestPublisher.publish(request);

        LOG.debug("Metadata request published for batch {}", batchId);
    }

    /**
     * Completes the enrichment process when metadata response is received from Kafka.
     * Retrieves cached events from Redis, enriches them with metadata, and continues processing.
     *
     * @param response The metadata batch response from Kafka
     */
    public void completeEnrichment(MetadataBatchResponse response) {
        if (response == null) {
            LOG.error("Received null metadata response");
            return;
        }

        String batchId = response.batchId();
        LOG.info("Completing enrichment for batch {}", batchId);

        // Retrieve cached events from Redis
        List<InputEventVo> events = cacheService.retrieveBatch(batchId);
        if (events == null || events.isEmpty()) {
            LOG.error("No cached events found for batch {}", batchId);
            return;
        }

        // Get metadata map from response
        Map<UUID, DeviceMetadataResTo> metadataMap = response.metadata();
        if (metadataMap == null) {
            LOG.error("No metadata in response for batch {}", batchId);
            cacheService.removeBatch(batchId);
            return;
        }

        LOG.info("Enriching {} events with {} metadata entries for batch {}",
                 events.size(), metadataMap.size(), batchId);

        // Enrich each event using the metadata
        List<ProcessedEventVo> processedEvents = new ArrayList<>();
        for (InputEventVo event : events) {
            try {
                DeviceMetadataResTo metadata = metadataMap.get(event.getDeviceId());
                if (metadata == null) {
                    LOG.error("No metadata found for device: {}, skipping event",
                        event.getDeviceId());
                    continue;
                }
                ProcessedEventVo vo = ProcessedEventVoMapper.MapFrom(event, metadata);
                processedEvents.add(vo);
            } catch (Exception e) {
                LOG.error("Failed to enrich event from device: {}. Error: {}",
                    event.getDeviceId(), e.getMessage(), e);
            }
        }

        LOG.info("Successfully enriched {} out of {} events for batch {}",
                 processedEvents.size(), events.size(), batchId);

        // Process and publish each enriched event
        for (ProcessedEventVo processedEvent : processedEvents) {
            try {
                processor.process(processedEvent);

                // Map to ProcessedEventTo
                ProcessedEventTo processedEventTo = ProcessedEventToMapper.MapFrom(processedEvent);

                // Publish to Kafka
                eventPublisher.publish(processedEventTo);

                LOG.debug("Successfully processed and published event from device: {}",
                         processedEvent.getDeviceId());
            } catch (Exception e) {
                LOG.error("Failed to process event from device: {}. Error: {}",
                         processedEvent.getDeviceId(), e.getMessage(), e);
            }
        }

        // Clean up: remove batch from cache (batch ID no longer needed)
        cacheService.removeBatch(batchId);

        LOG.info("Completed enrichment and processing for batch {}", batchId);
    }
}
