package cz.muni.fi.pv217.devicemanagementservice.kafka.pojos;


import java.util.Map;

public record BatchResponse(
        String batchId,
        Map<java.util.UUID, DeviceMetadata> metadata
) {}
