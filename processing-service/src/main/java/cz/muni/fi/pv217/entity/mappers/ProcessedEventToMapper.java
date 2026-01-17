package cz.muni.fi.pv217.entity.mappers;

import cz.muni.fi.pv217.entity.vo.ProcessedEventMetric;
import cz.muni.fi.pv217.entity.to.ProcessedEventTo;
import cz.muni.fi.pv217.entity.vo.ProcessedEventVo;
import java.util.List;

public class ProcessedEventToMapper {

    public static ProcessedEventTo MapFrom(ProcessedEventVo event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        return ProcessedEventTo.builder()
            .deviceId(event.getDeviceId())
            .timestamp(event.getTimestamp())
            .metrics(mapMetrics(event.getMetrics()))
            .location(event.getLocation())
            .deviceName(event.getDeviceName())
            .deviceType(event.getDeviceType())
            .deviceStatus(event.getDeviceStatus())
            .rules(mapRules(event.getRules()))
            .build();
    }

    private static List<ProcessedEventTo.Rule> mapRules(List<ProcessedEventVo.DeviceRule> rules) {
        if (rules == null) {
            return null;
        }
        return rules.stream()
            .map(ProcessedEventToMapper::mapRule)
            .toList();
    }

    private static ProcessedEventTo.Rule mapRule(ProcessedEventVo.DeviceRule rule) {
        if (rule == null) {
            return null;
        }
        return ProcessedEventTo.Rule.builder()
            .ruleName(rule.getRuleName())
            .from(rule.getFrom())
            .to(rule.getTo())
            .build();
    }

    private static List<ProcessedEventTo.ProcessedEventMetricTo> mapMetrics(List<ProcessedEventMetric> metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("Metrics list cannot be null");
        }
        return metrics.stream()
            .map(ProcessedEventToMapper::mapMetric)
            .toList();
    }

    private static ProcessedEventTo.ProcessedEventMetricTo mapMetric(ProcessedEventMetric metric) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }
        return ProcessedEventTo.ProcessedEventMetricTo.builder()
            .metricName(metric.getMetricName())
            .value(metric.getValue())
            .isViolatingSafety(metric.isViolatingSafety())
            .build();
    }
}
