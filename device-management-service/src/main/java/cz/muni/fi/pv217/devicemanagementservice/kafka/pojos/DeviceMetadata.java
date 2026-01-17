package cz.muni.fi.pv217.devicemanagementservice.kafka.pojos;

import java.util.List;
import java.util.Map;


public record DeviceMetadata(
        Map<String, Double> location,
        String deviceName,
        String deviceType,
        String deviceStatus,
        List<RuleData> rules
) {}
