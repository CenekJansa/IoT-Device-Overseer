package cz.muni.fi.pv217.entity.vo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ProcessedEventVo {
    UUID deviceId;
    Instant timestamp;
    List<ProcessedEventMetric> metrics;

    // Device metadata
    Map<String, Double> location;
    String deviceName;
    String deviceType;
    String deviceStatus;
    List<DeviceRule> rules;

    @Builder
    @Getter
    @Setter
    public static class DeviceRule {
        String ruleName;
        Double from;
        Double to;
    }
}
