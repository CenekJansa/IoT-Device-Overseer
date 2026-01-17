package cz.muni.fi.pv217.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.entity.vo.InputEventReading;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchCacheService Unit Tests")
class BatchCacheServiceTest {

    @Mock
    private RedisDataSource redisDataSource;

    @Mock
    private ValueCommands<String, String> valueCommands;

    private ObjectMapper objectMapper;
    private BatchCacheService batchCacheService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(redisDataSource.value(String.class)).thenReturn(valueCommands);
        batchCacheService = new BatchCacheService(redisDataSource, objectMapper);
    }

    // ==================== Helper Methods ====================

    private InputEventVo createEvent(UUID deviceId, Instant timestamp, String metricName, Double value) {
        InputEventReading reading = InputEventReading.builder()
            .metricName(metricName)
            .value(value)
            .build();

        return InputEventVo.builder()
            .deviceId(deviceId)
            .timestamp(timestamp)
            .readings(Collections.singletonList(reading))
            .build();
    }

    private List<InputEventVo> createEventList() {
        return Arrays.asList(
            createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), "temperature", 25.5),
            createEvent(UUID.randomUUID(), Instant.ofEpochMilli(2000L), "humidity", 60.0),
            createEvent(UUID.randomUUID(), Instant.ofEpochMilli(3000L), "pressure", 1013.25)
        );
    }

    // ==================== Store Batch Tests ====================

    @Test
    @DisplayName("Should successfully store batch with valid batch ID and events")
    void testStoreBatch_Success() throws JsonProcessingException {
        // Arrange
        String batchId = "batch-123";
        List<InputEventVo> events = createEventList();
        String expectedKey = "batch:" + batchId;

        // Act
        batchCacheService.storeBatch(batchId, events);

        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(valueCommands).setex(keyCaptor.capture(), ttlCaptor.capture(), valueCaptor.capture());
        assertEquals(expectedKey, keyCaptor.getValue(), "Key should have correct prefix");
        assertEquals(600L, ttlCaptor.getValue(), "TTL should be 10 minutes (600 seconds)");

        // Verify the JSON value can be deserialized back
        String jsonValue = valueCaptor.getValue();
        List<InputEventVo> deserializedEvents = objectMapper.readValue(
            jsonValue,
            new TypeReference<List<InputEventVo>>() {}
        );
        assertEquals(3, deserializedEvents.size(), "Should have 3 events");
    }

    @Test
    @DisplayName("Should successfully store batch with single event")
    void testStoreBatch_SingleEvent_Success() {
        // Arrange
        String batchId = "batch-456";
        List<InputEventVo> events = Collections.singletonList(
            createEvent(UUID.randomUUID(), Instant.ofEpochMilli(1000L), "temperature", 25.5)
        );

        // Act
        batchCacheService.storeBatch(batchId, events);

        // Assert
        verify(valueCommands).setex(eq("batch:batch-456"), eq(600L), anyString());
    }

    @Test
    @DisplayName("Should handle storing empty events list")
    void testStoreBatch_EmptyEvents() {
        // Arrange
        String batchId = "batch-789";
        List<InputEventVo> events = Collections.emptyList();

        // Act
        batchCacheService.storeBatch(batchId, events);

        // Assert
        verify(valueCommands).setex(eq("batch:batch-789"), eq(600L), anyString());
    }

    @Test
    @DisplayName("Should not store batch when batch ID is null")
    void testStoreBatch_NullBatchId() {
        // Arrange
        List<InputEventVo> events = createEventList();

        // Act
        batchCacheService.storeBatch(null, events);

        // Assert
        verify(valueCommands, never()).setex(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should not store batch when batch ID is empty")
    void testStoreBatch_EmptyBatchId() {
        // Arrange
        List<InputEventVo> events = createEventList();

        // Act
        batchCacheService.storeBatch("", events);

        // Assert
        verify(valueCommands, never()).setex(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should handle storing null events list")
    void testStoreBatch_NullEvents() {
        // Arrange
        String batchId = "batch-null";

        // Act
        batchCacheService.storeBatch(batchId, null);

        // Assert
        verify(valueCommands).setex(eq("batch:batch-null"), eq(600L), anyString());
    }

    @Test
    @DisplayName("Should handle JSON serialization error gracefully")
    void testStoreBatch_JsonProcessingException() throws JsonProcessingException {
        // Arrange
        String batchId = "batch-error";
        List<InputEventVo> events = createEventList();
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        BatchCacheService serviceWithMockMapper = new BatchCacheService(redisDataSource, mockMapper);

        // Act
        serviceWithMockMapper.storeBatch(batchId, events);

        // Assert
        verify(valueCommands, never()).setex(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should handle Redis operation error gracefully")
    void testStoreBatch_RedisException() {
        // Arrange
        String batchId = "batch-redis-error";
        List<InputEventVo> events = createEventList();
        doThrow(new RuntimeException("Redis connection error"))
            .when(valueCommands).setex(anyString(), anyLong(), anyString());

        // Act & Assert
        assertDoesNotThrow(() -> batchCacheService.storeBatch(batchId, events),
            "Should handle Redis exception gracefully");
    }

    // ==================== Retrieve Batch Tests ====================

    @Test
    @DisplayName("Should successfully retrieve batch with valid batch ID")
    void testRetrieveBatch_Success() throws JsonProcessingException {
        // Arrange
        String batchId = "batch-123";
        List<InputEventVo> expectedEvents = createEventList();
        String jsonValue = objectMapper.writeValueAsString(expectedEvents);
        when(valueCommands.get("batch:" + batchId)).thenReturn(jsonValue);

        // Act
        List<InputEventVo> result = batchCacheService.retrieveBatch(batchId);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should have 3 events");
        verify(valueCommands).get("batch:" + batchId);
    }

    @Test
    @DisplayName("Should return null when batch is not found in cache")
    void testRetrieveBatch_NotFound() {
        // Arrange
        String batchId = "batch-nonexistent";
        when(valueCommands.get("batch:" + batchId)).thenReturn(null);

        // Act
        List<InputEventVo> result = batchCacheService.retrieveBatch(batchId);

        // Assert
        assertNull(result, "Result should be null when batch not found");
        verify(valueCommands).get("batch:" + batchId);
    }

    @Test
    @DisplayName("Should return null when batch ID is null")
    void testRetrieveBatch_NullBatchId() {
        // Act
        List<InputEventVo> result = batchCacheService.retrieveBatch(null);

        // Assert
        assertNull(result, "Result should be null for null batch ID");
        verify(valueCommands, never()).get(anyString());
    }

    @Test
    @DisplayName("Should return null when batch ID is empty")
    void testRetrieveBatch_EmptyBatchId() {
        // Act
        List<InputEventVo> result = batchCacheService.retrieveBatch("");

        // Assert
        assertNull(result, "Result should be null for empty batch ID");
        verify(valueCommands, never()).get(anyString());
    }

    @Test
    @DisplayName("Should handle JSON deserialization error gracefully")
    void testRetrieveBatch_JsonProcessingException() throws JsonProcessingException {
        // Arrange
        String batchId = "batch-error";
        when(valueCommands.get("batch:" + batchId)).thenReturn("invalid json");

        // Act
        List<InputEventVo> result = batchCacheService.retrieveBatch(batchId);

        // Assert
        assertNull(result, "Result should be null on deserialization error");
    }

    @Test
    @DisplayName("Should handle Redis operation error gracefully during retrieve")
    void testRetrieveBatch_RedisException() {
        // Arrange
        String batchId = "batch-redis-error";
        when(valueCommands.get(anyString())).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        List<InputEventVo> result = batchCacheService.retrieveBatch(batchId);

        // Assert
        assertNull(result, "Result should be null on Redis error");
    }

    // ==================== Remove Batch Tests ====================

    @Test
    @DisplayName("Should successfully remove batch with valid batch ID")
    void testRemoveBatch_Success() {
        // Arrange
        String batchId = "batch-123";

        // Act
        batchCacheService.removeBatch(batchId);

        // Assert
        verify(valueCommands).getdel("batch:" + batchId);
    }

    @Test
    @DisplayName("Should not remove batch when batch ID is null")
    void testRemoveBatch_NullBatchId() {
        // Act
        batchCacheService.removeBatch(null);

        // Assert
        verify(valueCommands, never()).getdel(anyString());
    }

    @Test
    @DisplayName("Should not remove batch when batch ID is empty")
    void testRemoveBatch_EmptyBatchId() {
        // Act
        batchCacheService.removeBatch("");

        // Assert
        verify(valueCommands, never()).getdel(anyString());
    }

    @Test
    @DisplayName("Should handle Redis operation error gracefully during remove")
    void testRemoveBatch_RedisException() {
        // Arrange
        String batchId = "batch-redis-error";
        doThrow(new RuntimeException("Redis connection error"))
            .when(valueCommands).getdel(anyString());

        // Act & Assert
        assertDoesNotThrow(() -> batchCacheService.removeBatch(batchId),
            "Should handle Redis exception gracefully");
    }
}

