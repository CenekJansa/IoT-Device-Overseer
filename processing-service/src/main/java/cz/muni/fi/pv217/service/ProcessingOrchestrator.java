package cz.muni.fi.pv217.service;

import cz.muni.fi.pv217.entity.to.InputEventTo;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import cz.muni.fi.pv217.entity.mappers.InputEventVoMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the processing of telemetry events.
 * This service coordinates the enrichment via async Kafka messaging.
 * The actual processing and publishing happens in EventDataEnricher after metadata response arrives.
 */
@ApplicationScoped
public class ProcessingOrchestrator {
    private final EventDataEnricher enricher;

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingOrchestrator.class);

    public ProcessingOrchestrator(EventDataEnricher enricher) {
        this.enricher = enricher;
    }

    /**
     * Processes a batch of incoming telemetry events.
     * This method initiates async batch enrichment via Kafka messaging.
     * The actual processing and publishing will be completed when metadata response arrives.
     *
     * @param events The list of input events to process
     */
    public void processBatch(List<InputEventTo> events) {
        if (events == null || events.isEmpty()) {
            LOG.warn("Received null or empty batch, skipping processing");
            return;
        }

        LOG.info("Processing batch of {} telemetry events", events.size());

        // Convert all events to InputEventVo
        List<InputEventVo> inputEventVos = new ArrayList<>();
        for (InputEventTo event : events) {
            try {
                InputEventVo inputEventVo = InputEventVoMapper.MapFrom(event);
                inputEventVos.add(inputEventVo);
            } catch (Exception e) {
                LOG.error("Failed to map event from device: {}. Error: {}",
                         event.deviceId(), e.getMessage(), e);
            }
        }

        if (inputEventVos.isEmpty()) {
            LOG.warn("No valid events to process after mapping");
            return;
        }

        // Initiate async batch enrichment (will be completed when Kafka response arrives)
        enricher.enrichBatch(inputEventVos);

        LOG.info("Initiated async enrichment for batch of {} events", inputEventVos.size());
    }
}
