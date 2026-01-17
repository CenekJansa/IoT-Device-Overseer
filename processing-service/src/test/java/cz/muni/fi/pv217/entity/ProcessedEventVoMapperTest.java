package cz.muni.fi.pv217.entity;

import cz.muni.fi.pv217.entity.to.DeviceMetadataResTo;
import cz.muni.fi.pv217.entity.mappers.ProcessedEventVoMapper;
import cz.muni.fi.pv217.entity.vo.InputEventReading;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import cz.muni.fi.pv217.entity.vo.ProcessedEventMetric;
import cz.muni.fi.pv217.entity.vo.ProcessedEventVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessedEventVoMapper Unit Tests")
class ProcessedEventVoMapperTest {

    @Test
    @DisplayName("Should successfully map InputEventVo to ProcessedEventVo with matching rules")
    void testMapFrom_WithMatchingRules_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        InputEventReading reading1 = InputEventReading.builder()
            .metricName("temperature")
            .value(25.5)
            .build();

        InputEventReading reading2 = InputEventReading.builder()
            .metricName("humidity")
            .value(60.0)
            .build();

        InputEventVo event = InputEventVo.builder()
            .deviceId(deviceId)
            .timestamp(Instant.ofEpochMilli(1000L))
            .readings(Arrays.asList(reading1, reading2))
            .build();

        DeviceMetadataResTo.Rule rule1 = new DeviceMetadataResTo.Rule("temperature", 15.0, 30.0);
        DeviceMetadataResTo.Rule rule2 = new DeviceMetadataResTo.Rule("humidity", 40.0, 80.0);

        Map<String, Double> location = new HashMap<>();
        location.put("latitude", 49.1951);
        location.put("longitude", 16.6068);

        DeviceMetadataResTo metadata = new DeviceMetadataResTo(
            location,
            "Sensor-A1",
            "TemperatureSensor",
            "active",
            Arrays.asList(rule1, rule2)
        );

        // Act
        ProcessedEventVo result = ProcessedEventVoMapper.MapFrom(event, metadata);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(Instant.ofEpochMilli(1000L), result.getTimestamp(), "Timestamp should match");
        assertNotNull(result.getMetrics(), "Metrics should not be null");
        assertEquals(2, result.getMetrics().size(), "Should have two metrics");

        ProcessedEventMetric metric1 = result.getMetrics().get(0);
        assertEquals("temperature", metric1.getMetricName(), "First metric name should match");
        assertEquals(25.5, metric1.getValue(), "First metric value should match");
        assertEquals(15.0, metric1.getFrom(), "First metric from should match");
        assertEquals(30.0, metric1.getTo(), "First metric to should match");

        ProcessedEventMetric metric2 = result.getMetrics().get(1);
        assertEquals("humidity", metric2.getMetricName(), "Second metric name should match");
        assertEquals(60.0, metric2.getValue(), "Second metric value should match");
        assertEquals(40.0, metric2.getFrom(), "Second metric from should match");
        assertEquals(80.0, metric2.getTo(), "Second metric to should match");
    }

    @Test
    @DisplayName("Should map metric without matching rule with null range values")
    void testMapFrom_WithoutMatchingRule_Success() {
        // Arrange
        InputEventReading reading = InputEventReading.builder()
            .metricName("pressure")
            .value(1013.25)
            .build();
        
        InputEventVo event = InputEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(2000L))
            .readings(Collections.singletonList(reading))
            .build();

        DeviceMetadataResTo.Rule rule = new DeviceMetadataResTo.Rule("temperature", 15.0, 30.0);

        DeviceMetadataResTo metadata = new DeviceMetadataResTo(
            Collections.emptyMap(),
            "Sensor-B1",
            "PressureSensor",
            "active",
            Collections.singletonList(rule)
        );

        // Act
        ProcessedEventVo result = ProcessedEventVoMapper.MapFrom(event, metadata);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getMetrics().size(), "Should have one metric");
        
        ProcessedEventMetric metric = result.getMetrics().get(0);
        assertEquals("pressure", metric.getMetricName(), "Metric name should match");
        assertEquals(1013.25, metric.getValue(), "Metric value should match");
        assertNull(metric.getFrom(), "From should be null when no matching rule");
        assertNull(metric.getTo(), "To should be null when no matching rule");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when event is null")
    void testMapFrom_NullEvent_ThrowsException() {
        // Arrange
        DeviceMetadataResTo metadata = new DeviceMetadataResTo(
            Collections.emptyMap(),
            "Sensor-A1",
            "TemperatureSensor",
            "active",
            Collections.emptyList()
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ProcessedEventVoMapper.MapFrom(null, metadata),
            "Should throw IllegalArgumentException for null event"
        );
        assertEquals("Event cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when metadata is null")
    void testMapFrom_NullMetadata_ThrowsException() {
        // Arrange
        InputEventVo event = InputEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(1000L))
            .readings(Collections.emptyList())
            .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ProcessedEventVoMapper.MapFrom(event, null),
            "Should throw IllegalArgumentException for null metadata"
        );
        assertEquals("Metadata cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rules are null")
    void testMapFrom_NullRules_ThrowsException() {
        // Arrange
        InputEventReading reading = InputEventReading.builder()
            .metricName("temperature")
            .value(25.5)
            .build();
        
        InputEventVo event = InputEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(1000L))
            .readings(Collections.singletonList(reading))
            .build();

        DeviceMetadataResTo metadata = new DeviceMetadataResTo(
            Collections.emptyMap(),
            "Sensor-A1",
            "TemperatureSensor",
            "active",
            null
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ProcessedEventVoMapper.MapFrom(event, metadata),
            "Should throw IllegalArgumentException for null rules"
        );
        assertEquals("Rules not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rules are empty")
    void testMapFrom_EmptyRules_ThrowsException() {
        // Arrange
        InputEventReading reading = InputEventReading.builder()
            .metricName("temperature")
            .value(25.5)
            .build();

        InputEventVo event = InputEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(1000L))
            .readings(Collections.singletonList(reading))
            .build();

        DeviceMetadataResTo metadata = new DeviceMetadataResTo(
            Collections.emptyMap(),
            "Sensor-A1",
            "TemperatureSensor",
            "active",
            Collections.emptyList()
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ProcessedEventVoMapper.MapFrom(event, metadata),
            "Should throw IllegalArgumentException for empty rules"
        );
        assertEquals("Rules not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when reading is null")
    void testMapFrom_NullReading_ThrowsException() {
        // Arrange
        InputEventVo event = InputEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(1000L))
            .readings(Arrays.asList(
                InputEventReading.builder().metricName("temperature").value(25.5).build(),
                null
            ))
            .build();

        DeviceMetadataResTo.Rule rule = new DeviceMetadataResTo.Rule("temperature", 15.0, 30.0);
        DeviceMetadataResTo metadata = new DeviceMetadataResTo(
            Collections.emptyMap(),
            "Sensor-A1",
            "TemperatureSensor",
            "active",
            Collections.singletonList(rule)
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ProcessedEventVoMapper.MapFrom(event, metadata),
            "Should throw IllegalArgumentException for null reading"
        );
        assertEquals("Reading cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rule is null")
    void testMapFrom_NullRule_ThrowsException() {
        // Arrange
        InputEventReading reading = InputEventReading.builder()
            .metricName("temperature")
            .value(25.5)
            .build();

        InputEventVo event = InputEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(1000L))
            .readings(Collections.singletonList(reading))
            .build();

        // Put null rule first so it's encountered before finding a match
        DeviceMetadataResTo metadata = new DeviceMetadataResTo(
            Collections.emptyMap(),
            "Sensor-A1",
            "TemperatureSensor",
            "active",
            Arrays.asList(null, new DeviceMetadataResTo.Rule("temperature", 15.0, 30.0))
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ProcessedEventVoMapper.MapFrom(event, metadata),
            "Should throw IllegalArgumentException for null rule"
        );
        assertEquals("Rule cannot be null", exception.getMessage());
    }
}

