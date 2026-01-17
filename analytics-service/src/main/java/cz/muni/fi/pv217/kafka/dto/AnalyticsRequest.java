package cz.muni.fi.pv217.kafka.dto;

import java.util.UUID;
import java.util.Map;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AnalyticsRequest {

    UUID deviceId;
    Instant timestamp;
    List<ProcessedEventMetricTo> metrics;

    // Device metadata attributes from DeviceMetadataResTo
    Map<String, Double> location;
    String deviceName;
    String deviceType;
    String deviceStatus;
    List<Rule> rules;

    @Builder
    @Getter
    @Setter
    public static class ProcessedEventMetricTo {
        String metricName;
        Double value;
        boolean isViolatingSafety;
    }

    @Builder
    @Getter
    @Setter
    public static class Rule {
        String ruleName;
        Double from;
        Double to;
    }
}
