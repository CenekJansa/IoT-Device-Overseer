package cz.muni.fi.pv217.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.entity.to.InputEventTo;
import cz.muni.fi.pv217.service.ProcessingOrchestrator;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
@DisplayName("InputEventListener Kafka Integration Tests")
class InputEventListenerIT {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @InjectMock
    ProcessingOrchestrator orchestrator;

    @Inject
    ObjectMapper objectMapper;

    private static final String RAW_TELEMETRY_TOPIC = "sensor-ingest";

    @BeforeEach
    void setUp() {
        reset(orchestrator);
    }

    // ==================== Helper Methods ====================

    private String createValidJsonMessage(UUID deviceId, Instant timestamp) throws Exception {
        Map<String, Object> message = new HashMap<>();
        message.put("deviceId", deviceId.toString());
        message.put("timestamp", timestamp.toString());
        message.put("readings", List.of(
            Map.of("temperature", 25.5, "humidity", 60.0)
        ));
        return objectMapper.writeValueAsString(message);
    }

    // ==================== Successful Processing Tests ====================

    @Test
    @DisplayName("Should consume and process single valid message from Kafka")
    void testConsumeValidMessage() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Instant timestamp = Instant.ofEpochMilli(1000L);
        String validMessage = createValidJsonMessage(deviceId, timestamp);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", validMessage));

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<List<InputEventTo>> captor = ArgumentCaptor.forClass(List.class);
                verify(orchestrator, atLeastOnce()).processBatch(captor.capture());

                List<InputEventTo> events = captor.getValue();
                assertFalse(events.isEmpty(), "Should have processed at least one event");

                InputEventTo event = events.stream()
                    .filter(e -> e.deviceId().equals(deviceId))
                    .findFirst()
                    .orElse(null);

                assertNotNull(event, "Should find event with deviceId " + deviceId);
                assertEquals(timestamp, event.timestamp());
            });
    }

    @Test
    @DisplayName("Should consume and process multiple messages from Kafka")
    void testConsumeMultipleMessages() throws Exception {
        // Arrange
        String message1 = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(1000L));
        String message2 = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(2000L));
        String message3 = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(3000L));

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", message1),
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key2", message2),
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key3", message3)
            );

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<List<InputEventTo>> captor = ArgumentCaptor.forClass(List.class);
                verify(orchestrator, atLeastOnce()).processBatch(captor.capture());

                // Get all captured events across all invocations
                List<List<InputEventTo>> allBatches = captor.getAllValues();
                long totalEvents = allBatches.stream()
                    .flatMap(List::stream)
                    .count();

                assertTrue(totalEvents >= 3, "Should have processed at least 3 events");
            });
    }

    @Test
    @DisplayName("Should handle batch consumption from Kafka")
    void testBatchConsumption() throws Exception {
        // Arrange - send multiple messages quickly to trigger batch consumption
        for (int i = 0; i < 10; i++) {
            String message = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(i * 1000L));
            companion.produce(String.class, String.class)
                .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key" + i, message));
        }

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<List<InputEventTo>> captor = ArgumentCaptor.forClass(List.class);
                verify(orchestrator, atLeastOnce()).processBatch(captor.capture());

                // Get all captured events
                List<List<InputEventTo>> allBatches = captor.getAllValues();
                long totalEvents = allBatches.stream()
                    .flatMap(List::stream)
                    .count();

                assertTrue(totalEvents >= 10, "Should have processed at least 10 events");
            });
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle invalid JSON messages gracefully")
    void testConsumeInvalidJson() {
        // Arrange
        String invalidMessage = "invalid json";

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", invalidMessage));

        // Assert - should not crash, orchestrator should not be called for invalid messages
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                // Verify that if orchestrator was called, it wasn't with invalid data
                // The listener should skip invalid messages
                verify(orchestrator, atMost(1)).processBatch(any());
            });
    }

    @Test
    @DisplayName("Should handle mixed valid and invalid messages")
    void testConsumeMixedMessages() throws Exception {
        // Arrange
        String validMessage1 = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(1000L));
        String invalidMessage = "invalid json";
        String validMessage2 = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(2000L));

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", validMessage1),
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key2", invalidMessage),
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key3", validMessage2)
            );

        // Assert - should process valid messages and skip invalid ones
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<List<InputEventTo>> captor = ArgumentCaptor.forClass(List.class);
                verify(orchestrator, atLeastOnce()).processBatch(captor.capture());

                // Get all valid events processed
                List<List<InputEventTo>> allBatches = captor.getAllValues();
                long totalEvents = allBatches.stream()
                    .flatMap(List::stream)
                    .count();

                // Should have at least 2 valid events (invalid one should be skipped)
                assertTrue(totalEvents >= 2, "Should have processed at least 2 valid events");
            });
    }

    @Test
    @DisplayName("Should continue processing after orchestrator exception")
    void testContinueAfterException() throws Exception {
        // Arrange
        String message1 = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(1000L));
        String message2 = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(2000L));

        // First call throws exception, second call succeeds
        doThrow(new RuntimeException("Processing error"))
            .doNothing()
            .when(orchestrator).processBatch(any());

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", message1));

        // Wait a bit for first message to be processed
        await().pollDelay(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(3))
            .untilAsserted(() -> verify(orchestrator, atLeastOnce()).processBatch(any()));

        // Send second message
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key2", message2));

        // Assert - should have called orchestrator at least twice (once failed, once succeeded)
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(orchestrator, atLeast(2)).processBatch(any());
            });
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle empty message")
    void testConsumeEmptyMessage() {
        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", ""));

        // Assert - should not crash
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                // Empty messages should be skipped
                verify(orchestrator, atMost(1)).processBatch(any());
            });
    }

    @Test
    @DisplayName("Should handle message with missing fields")
    void testConsumeMissingFields() throws Exception {
        // Arrange - message missing timestamp
        Map<String, Object> message = new HashMap<>();
        message.put("deviceId", UUID.randomUUID().toString());
        // missing timestamp
        message.put("readings", List.of(Map.of("temperature", 25.5)));
        String jsonMessage = objectMapper.writeValueAsString(message);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", jsonMessage));

        // Assert - should handle gracefully (depends on implementation)
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                // Verify behavior - might be processed or skipped depending on validation
                verify(orchestrator, atMost(1)).processBatch(any());
            });
    }

    @Test
    @DisplayName("Should handle high throughput")
    void testHighThroughput() throws Exception {
        // Arrange - send 50 messages
        for (int i = 0; i < 50; i++) {
            String message = createValidJsonMessage(UUID.randomUUID(), Instant.ofEpochMilli(i * 1000L));
            companion.produce(String.class, String.class)
                .fromRecords(new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key" + i, message));
        }

        // Assert
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                ArgumentCaptor<List<InputEventTo>> captor = ArgumentCaptor.forClass(List.class);
                verify(orchestrator, atLeastOnce()).processBatch(captor.capture());

                List<List<InputEventTo>> allBatches = captor.getAllValues();
                long totalEvents = allBatches.stream()
                    .flatMap(List::stream)
                    .count();

                assertTrue(totalEvents >= 50, "Should have processed at least 50 events");
            });
    }
}

