package cz.muni.fi.pv217.kafka.dto;

import cz.muni.fi.pv217.entity.to.DeviceMetadataResTo;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for batch metadata responses received via Kafka.
 * Contains the batch ID and a map of device ID to metadata.
 */
public record MetadataBatchResponse(
    String batchId,
    Map<UUID, DeviceMetadataResTo> metadata
) {
}

