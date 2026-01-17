package cz.muni.fi.pv217.entity.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@JsonDeserialize(builder = InputEventVo.InputEventVoBuilder.class)
public class InputEventVo {
    UUID deviceId;
    Instant timestamp;
    List<InputEventReading> readings;

    @JsonPOJOBuilder(withPrefix = "")
    public static class InputEventVoBuilder {
    }
}
