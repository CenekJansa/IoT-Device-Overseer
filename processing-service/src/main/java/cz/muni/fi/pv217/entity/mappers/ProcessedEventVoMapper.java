package cz.muni.fi.pv217.entity.mappers;

import cz.muni.fi.pv217.entity.to.DeviceMetadataResTo;
import cz.muni.fi.pv217.entity.vo.InputEventReading;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import cz.muni.fi.pv217.entity.vo.ProcessedEventMetric;
import cz.muni.fi.pv217.entity.vo.ProcessedEventVo;
import java.util.ArrayList;
import java.util.List;

public class ProcessedEventVoMapper {

    public static ProcessedEventVo MapFrom(InputEventVo event, DeviceMetadataResTo metadata) {
        // Validate inputs
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        if (metadata.rules() == null || metadata.rules().isEmpty()) {
            throw new IllegalArgumentException("Rules not found");
        }
        if (event.getReadings() == null) {
            throw new IllegalArgumentException("Readings list cannot be null");
        }

        // Process each reading
        List<ProcessedEventMetric> metrics = new ArrayList<>();
        for (InputEventReading reading : event.getReadings()) {
            if (reading == null) {
                throw new IllegalArgumentException("Reading cannot be null");
            }
            ProcessedEventMetric metric = createMetric(reading, metadata.rules());
            metrics.add(metric);
        }

        // Map rules from metadata
        List<ProcessedEventVo.DeviceRule> deviceRules = metadata.rules().stream()
                .map(rule -> ProcessedEventVo.DeviceRule.builder()
                        .ruleName(rule.rule_name())
                        .from(rule.from())
                        .to(rule.to())
                        .build())
                .toList();

        // Build and return result with metadata
        return ProcessedEventVo.builder()
                .deviceId(event.getDeviceId())
                .timestamp(event.getTimestamp())
                .metrics(metrics)
                .location(metadata.location())
                .deviceName(metadata.deviceName())
                .deviceType(metadata.deviceType())
                .deviceStatus(metadata.deviceStatus())
                .rules(deviceRules)
                .build();
    }

    private static ProcessedEventMetric createMetric(InputEventReading reading, List<DeviceMetadataResTo.Rule> rules) {
        // Find matching rule
        DeviceMetadataResTo.Rule rule = findMatchingRule(reading.getMetricName(), rules);

        // Build metric with rule data if available
        ProcessedEventMetric.ProcessedEventMetricBuilder builder = ProcessedEventMetric.builder()
                .metricName(reading.getMetricName())
                .value(reading.getValue());

        if (rule != null) {
            builder.from(rule.from())
                   .to(rule.to());
        }

        return builder.build();
    }

    private static DeviceMetadataResTo.Rule findMatchingRule(String metricName, List<DeviceMetadataResTo.Rule> rules) {
        for (DeviceMetadataResTo.Rule rule : rules) {
            if (rule == null) {
                throw new IllegalArgumentException("Rule cannot be null");
            }
            if (metricName != null && metricName.equals(rule.rule_name())) {
                return rule;
            }
        }
        return null;
    }
}
