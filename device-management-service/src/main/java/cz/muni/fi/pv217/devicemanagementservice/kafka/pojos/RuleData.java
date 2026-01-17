package cz.muni.fi.pv217.devicemanagementservice.kafka.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RuleData (
    @JsonProperty("rule_name")
    String ruleName,
    double from,
    double to
) {}
