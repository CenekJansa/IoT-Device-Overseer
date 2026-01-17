package cz.muni.fi.pv217.entity;

import cz.muni.fi.pv217.entity.mappers.InputEventVoMapper;
import cz.muni.fi.pv217.entity.to.InputEventTo;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InputEventVoMapper Unit Tests")
class InputEventVoMapperTest {

    // ==================== Successful Mapping Tests ====================

    @Test
    @DisplayName("Should successfully map InputEventTo to InputEventVo with single reading")
    void testMapFrom_SingleReading_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Instant timestamp = Instant.ofEpochMilli(1000L);

        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", 25.5);
        reading.put("value", 25.5);

        InputEventTo inputEvent = new InputEventTo(
            deviceId,
            timestamp,
            Collections.singletonList(reading)
        );

        // Act
        InputEventVo result = InputEventVoMapper.MapFrom(inputEvent);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(timestamp, result.getTimestamp(), "Timestamp should match");
        assertNotNull(result.getReadings(), "Readings should not be null");
        // The mapper creates one reading per key in the map, so 2 keys = 2 readings
        assertEquals(2, result.getReadings().size(), "Should have two readings (one per key)");
        // All readings should have the value from the "value" key
        assertTrue(result.getReadings().stream().allMatch(r -> r.getValue().equals(25.5)),
                   "All readings should have value from 'value' key");
    }

    @Test
    @DisplayName("Should successfully map InputEventTo to InputEventVo with multiple readings")
    void testMapFrom_MultipleReadings_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Instant timestamp = Instant.ofEpochMilli(2000L);

        Map<String, Object> reading1 = new HashMap<>();
        reading1.put("temperature", 25.5);
        reading1.put("value", 25.5);

        Map<String, Object> reading2 = new HashMap<>();
        reading2.put("humidity", 60.0);
        reading2.put("value", 60.0);

        Map<String, Object> reading3 = new HashMap<>();
        reading3.put("pressure", 1013.25);
        reading3.put("value", 1013.25);

        InputEventTo inputEvent = new InputEventTo(
            deviceId,
            timestamp,
            Arrays.asList(reading1, reading2, reading3)
        );

        // Act
        InputEventVo result = InputEventVoMapper.MapFrom(inputEvent);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(timestamp, result.getTimestamp(), "Timestamp should match");
        assertNotNull(result.getReadings(), "Readings should not be null");
        // Each map has 2 keys, so 3 maps * 2 keys = 6 readings
        assertEquals(6, result.getReadings().size(), "Should have six readings (2 per map)");
    }

    @Test
    @DisplayName("Should map reading with multiple keys correctly")
    void testMapFrom_ReadingWithMultipleKeys_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Instant timestamp = Instant.ofEpochMilli(3000L);

        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", 25.5);
        reading.put("humidity", 60.0);
        reading.put("value", 25.5);

        InputEventTo inputEvent = new InputEventTo(
            deviceId,
            timestamp,
            Collections.singletonList(reading)
        );

        // Act
        InputEventVo result = InputEventVoMapper.MapFrom(inputEvent);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(timestamp, result.getTimestamp(), "Timestamp should match");
        assertNotNull(result.getReadings(), "Readings should not be null");
        // Should create one reading per key in the map
        assertTrue(result.getReadings().size() >= 1, "Should have at least one reading");
    }

    // ==================== Null Input Tests ====================

    @Test
    @DisplayName("Should throw IllegalArgumentException when event is null")
    void testMapFrom_NullEvent_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(null),
            "Should throw IllegalArgumentException for null event"
        );
        assertEquals("Event cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when deviceId is null")
    void testMapFrom_NullDeviceId_ThrowsException() {
        // Arrange
        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", 25.5);
        reading.put("value", 25.5);

        InputEventTo inputEvent = new InputEventTo(
            null,
            Instant.ofEpochMilli(1000L),
            Collections.singletonList(reading)
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException for null deviceId"
        );
        assertEquals("Device ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when timestamp is null")
    void testMapFrom_NullTimestamp_ThrowsException() {
        // Arrange
        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", 25.5);
        reading.put("value", 25.5);

        InputEventTo inputEvent = new InputEventTo(
            UUID.randomUUID(),
            null,
            Collections.singletonList(reading)
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException for null timestamp"
        );
        assertEquals("Timestamp cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when readings is null")
    void testMapFrom_NullReadings_ThrowsException() {
        // Arrange
        InputEventTo inputEvent = new InputEventTo(UUID.randomUUID(), Instant.ofEpochMilli(1000L), null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException for null readings"
        );
        assertEquals("Readings cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when readings is empty")
    void testMapFrom_EmptyReadings_ThrowsException() {
        // Arrange
        InputEventTo inputEvent = new InputEventTo(UUID.randomUUID(), Instant.ofEpochMilli(1000L), Collections.emptyList());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException for empty readings"
        );
        assertEquals("Readings cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when a reading is null")
    void testMapFrom_NullReading_ThrowsException() {
        // Arrange
        List<Map<String, Object>> readings = new ArrayList<>();
        readings.add(null);

        InputEventTo inputEvent = new InputEventTo(UUID.randomUUID(), Instant.ofEpochMilli(1000L), readings);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException for null reading"
        );
        assertEquals("Reading cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when a reading is empty")
    void testMapFrom_EmptyReading_ThrowsException() {
        // Arrange
        InputEventTo inputEvent = new InputEventTo(
            UUID.randomUUID(),
            Instant.ofEpochMilli(1000L),
            Collections.singletonList(Collections.emptyMap())
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException for empty reading"
        );
        assertEquals("Reading cannot be null", exception.getMessage());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle reading with only value key")
    void testMapFrom_ReadingWithOnlyValueKey_Success() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Instant timestamp = Instant.ofEpochMilli(4000L);

        Map<String, Object> reading = new HashMap<>();
        reading.put("value", 42.0);

        InputEventTo inputEvent = new InputEventTo(
            deviceId,
            timestamp,
            Collections.singletonList(reading)
        );

        // Act
        InputEventVo result = InputEventVoMapper.MapFrom(inputEvent);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(deviceId, result.getDeviceId(), "Device ID should match");
        assertEquals(timestamp, result.getTimestamp(), "Timestamp should match");
        assertNotNull(result.getReadings(), "Readings should not be null");
        assertEquals(1, result.getReadings().size(), "Should have one reading");
        assertEquals("value", result.getReadings().get(0).getMetricName(), "Metric name should be 'value'");
        assertEquals(42.0, result.getReadings().get(0).getValue(), "Value should match");
    }

    @Test
    @DisplayName("Should handle zero values in readings")
    void testMapFrom_ZeroValues_Success() {
        // Arrange
        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", 0.0);
        reading.put("value", 0.0);

        InputEventTo inputEvent = new InputEventTo(
            UUID.randomUUID(),
            Instant.ofEpochMilli(5000L),
            Collections.singletonList(reading)
        );

        // Act
        InputEventVo result = InputEventVoMapper.MapFrom(inputEvent);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(0.0, result.getReadings().get(0).getValue(), "Should handle zero value");
    }

    @Test
    @DisplayName("Should handle negative values in readings")
    void testMapFrom_NegativeValues_Success() {
        // Arrange
        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", -10.5);
        reading.put("value", -10.5);

        InputEventTo inputEvent = new InputEventTo(
            UUID.randomUUID(),
            Instant.ofEpochMilli(6000L),
            Collections.singletonList(reading)
        );

        // Act
        InputEventVo result = InputEventVoMapper.MapFrom(inputEvent);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(-10.5, result.getReadings().get(0).getValue(), "Should handle negative value");
    }

    @Test
    @DisplayName("Should handle very large values")
    void testMapFrom_LargeValues_Success() {
        // Arrange
        Map<String, Object> reading = new HashMap<>();
        reading.put("pressure", Double.MAX_VALUE);
        reading.put("value", Double.MAX_VALUE);

        InputEventTo inputEvent = new InputEventTo(
            UUID.randomUUID(),
            Instant.ofEpochMilli(7000L),
            Collections.singletonList(reading)
        );

        // Act
        InputEventVo result = InputEventVoMapper.MapFrom(inputEvent);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(Double.MAX_VALUE, result.getReadings().get(0).getValue(), "Should handle large value");
    }

    @Test
    @DisplayName("Should throw exception when reading map contains null key")
    void testMapFrom_NullKeyInReading_ThrowsException() {
        // Arrange
        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", 25.5);
        reading.put(null, 30.0);  // Null key in the map
        reading.put("value", 25.5);

        InputEventTo inputEvent = new InputEventTo(
            UUID.randomUUID(),
            Instant.ofEpochMilli(8000L),
            Collections.singletonList(reading)
        );

        // Act & Assert
        // This should throw an exception because entry.getKey() will be null
        // and we should validate that keys are not null
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException for null key in reading map"
        );
        assertEquals("Metric name cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when reading map has null value for 'value' key")
    void testMapFrom_NullValueForValueKey_ThrowsException() {
        // Arrange
        Map<String, Object> reading = new HashMap<>();
        reading.put("temperature", 25.5);
        reading.put("value", null);  // Null value for "value" key

        InputEventTo inputEvent = new InputEventTo(
            UUID.randomUUID(),
            Instant.ofEpochMilli(10000L),
            Collections.singletonList(reading)
        );

        // Act & Assert
        // This should throw an exception because the value is null
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> InputEventVoMapper.MapFrom(inputEvent),
            "Should throw IllegalArgumentException when 'value' key has null value"
        );
        assertEquals("Reading value cannot be null", exception.getMessage());
    }
}

