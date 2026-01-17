package cz.muni.fi.pv217.entity.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ProcessedEventMetric {
    private String metricName;
    private Double value;
    private Double from;
    private Double to;
    private boolean isViolatingSafety;
}
