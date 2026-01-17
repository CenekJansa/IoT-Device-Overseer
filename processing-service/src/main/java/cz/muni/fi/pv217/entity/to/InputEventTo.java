package cz.muni.fi.pv217.entity.to;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InputEventTo(
    UUID deviceId,
    Instant timestamp,
    List<Map<String, Object>> readings) {
}
