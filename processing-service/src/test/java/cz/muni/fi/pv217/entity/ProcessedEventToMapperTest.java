package cz.muni.fi.pv217.entity;

import cz.muni.fi.pv217.entity.mappers.ProcessedEventToMapper;
import cz.muni.fi.pv217.entity.to.ProcessedEventTo;
import cz.muni.fi.pv217.entity.vo.ProcessedEventMetric;
import cz.muni.fi.pv217.entity.vo.ProcessedEventVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessedEventToMapper Unit Tests")
class ProcessedEventToMapperTest {

    @Test
    @DisplayName("Should successfully map ProcessedEventVo to ProcessedEventTo with single metric")
    void testMapFrom_SingleMetric_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        ProcessedEventMetric metric = ProcessedEventMetric.builder()
            .metricName("temperature")
            .value(25.5)
            .from(15.0)
            .to(30.0)
            .isViolatingSafety(false)
            .build();

        Map<String, Double> location = new HashMap<>();
        location.put("latitude", 50.0);
        location.put("longitude", 14.0);

        ProcessedEventVo.DeviceRule rule = ProcessedEventVo.DeviceRule.builder()
            .ruleName("temperature")
            .from(15.0)
            .to(30.0)
            .build();

        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(deviceId)
            .timestamp(Instant.ofEpochMilli(1000L))
            .metrics(Collections.singletonList(metric))
            .location(location)
            .deviceName("Test Device")
            .deviceType("Sensor")
            .deviceStatus("ACTIVE")
            .rules(Collections.singletonList(rule))
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(Instant.ofEpochMilli(1000L), result.getTimestamp(), "Timestamp should match");
        assertNotNull(result.getMetrics(), "Metrics should not be null");
        assertEquals(1, result.getMetrics().size(), "Should have one metric");

        ProcessedEventTo.ProcessedEventMetricTo resultMetric = result.getMetrics().get(0);
        assertEquals("temperature", resultMetric.getMetricName(), "Metric name should match");
        assertEquals(25.5, resultMetric.getValue(), "Value should match");
        assertFalse(resultMetric.isViolatingSafety(), "Safety violation flag should match");

        // Assert metadata fields
        assertNotNull(result.getLocation(), "Location should not be null");
        assertEquals(50.0, result.getLocation().get("latitude"), "Latitude should match");
        assertEquals(14.0, result.getLocation().get("longitude"), "Longitude should match");
        assertEquals("Test Device", result.getDeviceName(), "Device name should match");
        assertEquals("Sensor", result.getDeviceType(), "Device type should match");
        assertEquals("ACTIVE", result.getDeviceStatus(), "Device status should match");
        assertNotNull(result.getRules(), "Rules should not be null");
        assertEquals(1, result.getRules().size(), "Should have one rule");
        assertEquals("temperature", result.getRules().get(0).getRuleName(), "Rule name should match");
        assertEquals(15.0, result.getRules().get(0).getFrom(), "Rule from should match");
        assertEquals(30.0, result.getRules().get(0).getTo(), "Rule to should match");
    }

    @Test
    @DisplayName("Should successfully map ProcessedEventVo to ProcessedEventTo with multiple metrics")
    void testMapFrom_MultipleMetrics_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        ProcessedEventMetric metric1 = ProcessedEventMetric.builder()
            .metricName("temperature")
            .value(25.5)
            .from(15.0)
            .to(30.0)
            .isViolatingSafety(false)
            .build();

        ProcessedEventMetric metric2 = ProcessedEventMetric.builder()
            .metricName("humidity")
            .value(60.0)
            .from(40.0)
            .to(80.0)
            .isViolatingSafety(false)
            .build();

        ProcessedEventMetric metric3 = ProcessedEventMetric.builder()
            .metricName("pressure")
            .value(1013.25)
            .from(950.0)
            .to(1050.0)
            .isViolatingSafety(false)
            .build();

        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(deviceId)
            .timestamp(Instant.ofEpochMilli(2000L))
            .metrics(Arrays.asList(metric1, metric2, metric3))
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(Instant.ofEpochMilli(2000L), result.getTimestamp(), "Timestamp should match");
        assertEquals(3, result.getMetrics().size(), "Should have three metrics");

        ProcessedEventTo.ProcessedEventMetricTo resultMetric1 = result.getMetrics().get(0);
        assertEquals("temperature", resultMetric1.getMetricName(), "First metric name should match");
        assertEquals(25.5, resultMetric1.getValue(), "First metric value should match");
        assertFalse(resultMetric1.isViolatingSafety(), "First metric safety flag should match");

        ProcessedEventTo.ProcessedEventMetricTo resultMetric2 = result.getMetrics().get(1);
        assertEquals("humidity", resultMetric2.getMetricName(), "Second metric name should match");
        assertEquals(60.0, resultMetric2.getValue(), "Second metric value should match");
        assertFalse(resultMetric2.isViolatingSafety(), "Second metric safety flag should match");

        ProcessedEventTo.ProcessedEventMetricTo resultMetric3 = result.getMetrics().get(2);
        assertEquals("pressure", resultMetric3.getMetricName(), "Third metric name should match");
        assertEquals(1013.25, resultMetric3.getValue(), "Third metric value should match");
        assertFalse(resultMetric3.isViolatingSafety(), "Third metric safety flag should match");
    }

    @Test
    @DisplayName("Should map metric with safety violation flag set")
    void testMapFrom_SafetyViolationSet_Success() {
        // Arrange
        ProcessedEventMetric metric = ProcessedEventMetric.builder()
            .metricName("temperature")
            .value(35.0)
            .from(15.0)
            .to(30.0)
            .isViolatingSafety(true)
            .build();

        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(3000L))
            .metrics(Collections.singletonList(metric))
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        ProcessedEventTo.ProcessedEventMetricTo resultMetric = result.getMetrics().get(0);
        assertTrue(resultMetric.isViolatingSafety(), "Safety violation flag should be true");
    }

    @Test
    @DisplayName("Should handle empty metrics list")
    void testMapFrom_EmptyMetrics_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(deviceId)
            .timestamp(Instant.ofEpochMilli(5000L))
            .metrics(Collections.emptyList())
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(Instant.ofEpochMilli(5000L), result.getTimestamp(), "Timestamp should match");
        assertNotNull(result.getMetrics(), "Metrics should not be null");
        assertEquals(0, result.getMetrics().size(), "Should have no metrics");
    }

    @Test
    @DisplayName("Should handle null value")
    void testMapFrom_NullValue_Success() {
        // Arrange
        ProcessedEventMetric metric = ProcessedEventMetric.builder()
            .metricName("temperature")
            .value(null)
            .from(15.0)
            .to(30.0)
            .isViolatingSafety(false)
            .build();

        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(6000L))
            .metrics(Collections.singletonList(metric))
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        ProcessedEventTo.ProcessedEventMetricTo resultMetric = result.getMetrics().get(0);
        assertNull(resultMetric.getValue(), "Should handle null value");
    }

    @Test
    @DisplayName("Should handle zero values")
    void testMapFrom_ZeroValues_Success() {
        // Arrange
        ProcessedEventMetric metric = ProcessedEventMetric.builder()
            .metricName("count")
            .value(0.0)
            .from(-10.0)
            .to(10.0)
            .isViolatingSafety(false)
            .build();

        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(8000L))
            .metrics(Collections.singletonList(metric))
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        ProcessedEventTo.ProcessedEventMetricTo resultMetric = result.getMetrics().get(0);
        assertEquals(0.0, resultMetric.getValue(), "Should handle zero value");
    }

    @Test
    @DisplayName("Should handle negative values")
    void testMapFrom_NegativeValues_Success() {
        // Arrange
        ProcessedEventMetric metric = ProcessedEventMetric.builder()
            .metricName("temperature")
            .value(-5.0)
            .from(-10.0)
            .to(0.0)
            .isViolatingSafety(false)
            .build();

        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(9000L))
            .metrics(Collections.singletonList(metric))
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        ProcessedEventTo.ProcessedEventMetricTo resultMetric = result.getMetrics().get(0);
        assertEquals(-5.0, resultMetric.getValue(), "Should handle negative value");
    }

    @Test
    @DisplayName("Should only map relevant fields (metricName, value, isViolatingSafety)")
    void testMapFrom_OnlyMapsRelevantFields_Success() {
        // Arrange
        ProcessedEventMetric metric = ProcessedEventMetric.builder()
            .metricName("temperature")
            .value(25.5)
            .from(15.0)  // Should not be mapped to ProcessedEventTo
            .to(30.0)    // Should not be mapped to ProcessedEventTo
            .isViolatingSafety(true)
            .build();

        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(11000L))
            .metrics(Collections.singletonList(metric))
            .build();

        // Act
        ProcessedEventTo result = ProcessedEventToMapper.MapFrom(event);

        // Assert
        assertNotNull(result, "Result should not be null");
        ProcessedEventTo.ProcessedEventMetricTo resultMetric = result.getMetrics().get(0);

        // Verify that only the expected fields are present
        assertEquals("temperature", resultMetric.getMetricName(), "Metric name should be mapped");
        assertEquals(25.5, resultMetric.getValue(), "Value should be mapped");
        assertTrue(resultMetric.isViolatingSafety(), "Safety violation flag should be mapped");
        // Note: from and to should NOT be in the output DTO
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when event is null")
    void testMapFrom_NullEvent_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ProcessedEventToMapper.MapFrom(null),
            "Should throw IllegalArgumentException for null event"
        );
        assertEquals("Event cannot be null", exception.getMessage());
    }
}

