package cz.muni.fi.pv217.kafka.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for metadata batch request sent to Kafka topic "metadata-batch-requests"
 */
public record MetadataBatchRequest(
    String batchId,
    List<UUID> deviceIds
) {
}

