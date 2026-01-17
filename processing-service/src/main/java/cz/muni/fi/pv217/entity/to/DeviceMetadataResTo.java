package cz.muni.fi.pv217.entity.to;

import java.util.List;
import java.util.Map;

public record DeviceMetadataResTo(
    Map<String, Double> location,
    String deviceName,
    String deviceType,
    String deviceStatus,
    List<Rule> rules
) {
    public record Rule(
        String rule_name,
        Double from,
        Double to
    ) {
    }
}
