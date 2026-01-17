package cz.muni.fi.pv217.device;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.common.constraint.NotNull;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@Getter
@Setter
public class DevicePayload {
    @JsonProperty("deviceId")
    @NotNull
    public UUID deviceId;

    @JsonProperty("timestamp")
    @NotNull
    public Instant timestamp;

    @JsonProperty("readings")
    public List<Map<String, Object>> readings;

    public DevicePayload() {}

    public DevicePayload(UUID deviceId, Instant timestamp, List<Map<String, Object>> readings) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.readings = readings;
    }
}
