package cz.muni.fi.pv217.devicemanagementservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.domain.Rule;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.DeviceNotFoundException;
import cz.muni.fi.pv217.devicemanagementservice.kafka.pojos.BatchResponse;
import cz.muni.fi.pv217.devicemanagementservice.kafka.pojos.DeviceMetadata;
import cz.muni.fi.pv217.devicemanagementservice.kafka.pojos.RuleData;
import cz.muni.fi.pv217.devicemanagementservice.service.DeviceService;
import cz.muni.fi.pv217.devicemanagementservice.service.RuleService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MOCK class for testing - returns hardcoded device metadata.
 * This class listens to metadata-batch-requests and responds with mock data.
 * DELETE THIS CLASS when implementing real device metadata lookup.
 */
@ApplicationScoped
public class DeviceMetadataHandler {

    @Inject
    DeviceService deviceService;

    @Inject
    RuleService ruleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(DeviceMetadataHandler.class);

    @Inject
    @Channel("metadata-batch-responses")
    Emitter<String> responseEmitter;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("MockDeviceMetadataHandler is starting up - MOCK MODE ACTIVE");
        LOG.warn("This is a MOCK implementation - delete this class when implementing real metadata lookup");
    }

    /**
     * Listens to metadata-batch-requests topic and responds with hardcoded mock data.
     * Always returns the same hardcoded JSON response regardless of input.
     */
    @ActivateRequestContext
    @Incoming("metadata-batch-requests")
    @Timed("onMetadataRequest_processing_time")
    @Counted("onMetadataRequest_request_count")
    public void onMetadataRequest(String message) throws Exception {
        if (message == null || message.isEmpty()) {
            LOG.debug("Received empty message, skipping");
            return;
        }

        LOG.info("Received metadata batch request (MOCK): {}", message);
        JsonNode rootNode = objectMapper.readTree(message);


        try {
            // Extract batchId from the incoming message (simple string parsing)
            String batchId = extractBatchId(rootNode);
            List<UUID> deviceIds = extractDeviceIds(rootNode);

            if (batchId.isEmpty()) {
                LOG.error("Could not extract batch ID from message or does not exists, skipping");
                return;
            }

            if (deviceIds.isEmpty()) {
                LOG.error("Could not extract device ID(s) from message or does not exists, skipping");
                return;
            }

            LOG.info("Processing MOCK metadata request for batch ID: {}", batchId);
            LOG.info("Contains list of device ID(s): {}", deviceIds);
            List<Device> deviceList = deviceIds.stream()
                    // Map uses the static wrapper function (which catches the exception)
                    .map(this::findDeviceSafely)
                    // Filter out the null values returned by the wrapper function
                    .filter(Objects::nonNull)
                    .toList();

            // Generate device metadata response
            String response = generateDeviceResponse(batchId, deviceList);

            if (response != null) {
                responseEmitter.send(response);
                LOG.info("Sent metadata response for batch ID: {}", batchId);
            } else {
                LOG.error("Failed to generate response for batch ID: {}, skipping send", batchId);
            }

        } catch (Exception e) {
            LOG.error("Error handling metadata request: {}", e.getMessage(), e);
        }
    }

    public String generateDeviceResponse(String batchId, List<Device> deviceList) {
        Map<UUID, DeviceMetadata> metadataMap = new HashMap<>();
        for (Device device : deviceList) {
            UUID deviceId = device.id;
            List<Rule> rules = ruleService.findRuleByDeviceId(deviceId);
            List<RuleData> ruleDataList = rules.stream()
                    .map(rule -> new RuleData(
                            rule.ruleName,
                            rule.fromValue,
                            rule.toValue
                    ))
                    .toList();

            // Create location as Map to match processing service expectations
            Map<String, Double> location = new HashMap<>();
            location.put("latitude", device.latitude);
            location.put("longitude", device.longitude);

            DeviceMetadata deviceMetadata = new DeviceMetadata(location, device.name, device.type, device.status.name(), ruleDataList);
            metadataMap.put(deviceId, deviceMetadata);
        }

        BatchResponse batchResponse = new BatchResponse(batchId, metadataMap);

        try {
            return objectMapper.writeValueAsString(batchResponse);
        } catch (Exception e) {
            LOG.error("Failed to serialize batch response for batch ID: {}. Error: {}", batchId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Simple extraction of batchId from JSON string.
     */
    private String extractBatchId(JsonNode rootNode) {
        JsonNode batchIdNode = rootNode.get("batchId");
        if (batchIdNode != null && batchIdNode.isTextual()) {
            return batchIdNode.asText();
        }

        return "";
    }

    private List<UUID> extractDeviceIds(JsonNode rootNode) {
        JsonNode deviceIdsNode = rootNode.get("deviceIds");
        if (deviceIdsNode != null && deviceIdsNode.isArray()) {
            try {
                List<String> uuidStrings = objectMapper.readerForListOf(String.class).readValue(deviceIdsNode);

                // 2. Convert the List<String> to List<UUID> using a stream
                return uuidStrings.stream()
                        .map(s -> {
                            try {
                                // Attempt to parse the string into a UUID object
                                return UUID.fromString(s);
                            } catch (IllegalArgumentException e) {
                                // Handle the error if a string is not a valid UUID format
                                System.err.println("Invalid UUID format encountered: " + s);
                                return null; // Return null for invalid entries
                            }
                        })
                        .filter(java.util.Objects::nonNull) // Filter out any nulls resulting from invalid formats
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Log error if array format is unexpected
                System.err.println("Error mapping deviceIds list (expected strings/UUIDs): " + e.getMessage());
            }
        }
        // Safely return an empty list if the field is missing or not an array
        return Collections.emptyList();
    }

    private Device findDeviceSafely(UUID uuid) {
        try {
            return deviceService.findDeviceById(uuid);
        } catch (DeviceNotFoundException e) {
            return null;
        }
    }




}

