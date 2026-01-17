package cz.muni.fi.pv217.service;

import cz.muni.fi.pv217.entity.vo.ProcessedEventMetric;
import cz.muni.fi.pv217.entity.vo.ProcessedEventVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetricEvaluator Unit Tests")
class MetricEvaluatorTest {

    private MetricEvaluator processor;

    @BeforeEach
    void setUp() {
        processor = new MetricEvaluator();
    }

    // ==================== Safety Violation Tests (Range-based) ====================

    @Test
    @DisplayName("Should detect safety violation when value is below range")
    void testSafetyViolation_BelowRange() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", 10.0, 15.0, 30.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertTrue(metric.isViolatingSafety(), "Should detect safety violation when value < from");
    }

    @Test
    @DisplayName("Should detect safety violation when value is above range")
    void testSafetyViolation_AboveRange() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", 35.0, 15.0, 30.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertTrue(metric.isViolatingSafety(), "Should detect safety violation when value > to");
    }

    @Test
    @DisplayName("Should not detect safety violation when value is within range")
    void testSafetyViolation_WithinRange() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", 20.0, 15.0, 30.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should not detect safety violation when value is within range");
    }

    @Test
    @DisplayName("Should not detect safety violation when value equals lower bound")
    void testSafetyViolation_EqualsLowerBound() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", 15.0, 15.0, 30.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should not detect safety violation when value == from");
    }

    @Test
    @DisplayName("Should not detect safety violation when value equals upper bound")
    void testSafetyViolation_EqualsUpperBound() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", 30.0, 15.0, 30.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should not detect safety violation when value == to");
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 15.0, 30.0, true",   // Below range
        "14.9, 15.0, 30.0, true",   // Just below range
        "15.0, 15.0, 30.0, false",  // At lower bound
        "20.0, 15.0, 30.0, false",  // Within range
        "30.0, 15.0, 30.0, false",  // At upper bound
        "30.1, 15.0, 30.0, true",   // Just above range
        "35.0, 15.0, 30.0, true"    // Above range
    })
    @DisplayName("Should correctly evaluate safety violations for various values")
    void testSafetyViolation_VariousValues(double value, double from, double to, boolean expectedViolation) {
        // Arrange
        ProcessedEventMetric metric = createMetric("test", value, from, to);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertEquals(expectedViolation, metric.isViolatingSafety(),
            String.format("Value=%.1f with range [%.1f, %.1f] should %s safety violation",
                value, from, to, expectedViolation ? "detect" : "not detect"));
    }

    @Test
    @DisplayName("Should process multiple metrics correctly")
    void testMultipleMetrics() {
        // Arrange
        ProcessedEventMetric metric1 = createMetric("temperature", 35.0, 15.0, 30.0);  // Above range
        ProcessedEventMetric metric2 = createMetric("humidity", 50.0, 40.0, 80.0);     // Within range
        ProcessedEventMetric metric3 = createMetric("pressure", 900.0, 950.0, 1050.0); // Below range
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Arrays.asList(metric1, metric2, metric3));

        // Act
        processor.process(event);

        // Assert
        assertTrue(metric1.isViolatingSafety(), "Temperature should violate safety (above range)");
        assertFalse(metric2.isViolatingSafety(), "Humidity should not violate safety (within range)");
        assertTrue(metric3.isViolatingSafety(), "Pressure should violate safety (below range)");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null value gracefully")
    void testNullValue() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", null, 15.0, 30.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should not detect violation when value is null");
    }

    @Test
    @DisplayName("Should handle null from boundary gracefully")
    void testNullFromBoundary() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", 25.0, null, 30.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should not detect violation when from is null");
    }

    @Test
    @DisplayName("Should handle null to boundary gracefully")
    void testNullToBoundary() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", 25.0, 15.0, null);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should not detect violation when to is null");
    }

    @Test
    @DisplayName("Should handle metric with all null values")
    void testMetricWithAllNullValues() {
        // Arrange
        ProcessedEventMetric metric = ProcessedEventMetric.builder()
            .metricName("test")
            .value(null)
            .from(null)
            .to(null)
            .build();
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act & Assert
        assertDoesNotThrow(() -> processor.process(event), "Should handle all null values gracefully");
        assertFalse(metric.isViolatingSafety(), "Should not detect violation when all values are null");
    }

    @Test
    @DisplayName("Should handle empty metrics list")
    void testEmptyMetricsList() {
        // Arrange
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.emptyList());

        // Act & Assert
        assertDoesNotThrow(() -> processor.process(event), "Should handle empty metrics list gracefully");
    }

    @Test
    @DisplayName("Should handle null metrics list")
    void testNullMetricsList() {
        // Arrange
        ProcessedEventVo event = ProcessedEventVo.builder()
            .deviceId(UUID.randomUUID())
            .timestamp(Instant.ofEpochMilli(1000L))
            .metrics(null)
            .build();

        // Act & Assert
        assertDoesNotThrow(() -> processor.process(event), "Should handle null metrics list gracefully");
    }

    @Test
    @DisplayName("Should handle zero values")
    void testZeroValues() {
        // Arrange
        ProcessedEventMetric metric = createMetric("count", 0.0, -10.0, 10.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should handle zero value within range");
    }

    @Test
    @DisplayName("Should handle negative values")
    void testNegativeValues() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", -5.0, -10.0, 0.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertFalse(metric.isViolatingSafety(), "Should handle negative value within range");
    }

    @Test
    @DisplayName("Should detect violation with negative value below range")
    void testNegativeValueBelowRange() {
        // Arrange
        ProcessedEventMetric metric = createMetric("temperature", -15.0, -10.0, 0.0);
        ProcessedEventVo event = createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.singletonList(metric));

        // Act
        processor.process(event);

        // Assert
        assertTrue(metric.isViolatingSafety(), "Should detect violation when negative value is below range");
    }

    // ==================== Helper Methods ====================

    private ProcessedEventMetric createMetric(String name, Double value, Double from, Double to) {
        return ProcessedEventMetric.builder()
            .metricName(name)
            .value(value)
            .from(from)
            .to(to)
            .build();
    }

    private ProcessedEventVo createEvent(UUID deviceId, Instant timestamp, java.util.List<ProcessedEventMetric> metrics) {
        return ProcessedEventVo.builder()
            .deviceId(deviceId)
            .timestamp(timestamp)
            .metrics(metrics)
            .build();
    }
}

