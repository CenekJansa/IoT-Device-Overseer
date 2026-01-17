package cz.muni.fi.pv217.entity.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@JsonDeserialize(builder = InputEventReading.InputEventReadingBuilder.class)
public class InputEventReading {
    private String metricName;
    private Double value;

    @JsonPOJOBuilder(withPrefix = "")
    public static class InputEventReadingBuilder {
    }
}
