package cz.muni.fi.pv217.entity.mappers;

import cz.muni.fi.pv217.entity.vo.InputEventReading;
import cz.muni.fi.pv217.entity.to.InputEventTo;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputEventVoMapper {

    public static InputEventVo MapFrom(InputEventTo event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.deviceId() == null) {
            throw new IllegalArgumentException("Device ID cannot be null");
        }
        if (event.timestamp() == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (event.readings() == null || event.readings().isEmpty()) {
            throw new IllegalArgumentException("Readings cannot be null");
        }
        List<InputEventReading> readings = new ArrayList<>();
        for (Map<String, Object> reading : event.readings()) {
            if (reading == null || reading.isEmpty()) {
                throw new IllegalArgumentException("Reading cannot be null");
            }
            for (Map.Entry<String, Object> entry : reading.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    throw new IllegalArgumentException("Metric name cannot be null");
                }
                if (entry.getValue() == null) {
                    throw new IllegalArgumentException("Reading value cannot be null");
                }
                Double value = null;
                if (entry.getValue() instanceof Double){
                    value = (Double)entry.getValue();
                }else{
                    value = Double.parseDouble(entry.getValue().toString());
                }
                if (value == null) {
                    throw new IllegalArgumentException("Reading value cannot be null");
                }
                InputEventReading eventReading = InputEventReading.builder()
                    .metricName(key)
                    .value(value)
                    .build();
                readings.add(eventReading);
            }
        }
        return InputEventVo.builder()
            .deviceId(event.deviceId())
            .timestamp(event.timestamp())
            .readings(readings)
            .build();
    }
}
