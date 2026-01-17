package cz.muni.fi.pv217.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.entity.to.DeviceMetadataResTo;
import cz.muni.fi.pv217.kafka.dto.MetadataBatchResponse;
import cz.muni.fi.pv217.service.EventDataEnricher;
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
@DisplayName("MetadataBatchResponseListener Kafka Integration Tests")
class MetadataBatchResponseListenerIT {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @InjectMock
    EventDataEnricher enricher;

    @Inject
    ObjectMapper objectMapper;

    private static final String METADATA_BATCH_RESPONSES_TOPIC = "metadata-batch-responses";

    @BeforeEach
    void setUp() {
        reset(enricher);
    }

    // ==================== Helper Methods ====================

    private DeviceMetadataResTo createDeviceMetadata(String deviceName, String deviceType) {
        Map<String, Double> location = new HashMap<>();
        location.put("latitude", 50.0);
        location.put("longitude", 14.0);

        DeviceMetadataResTo.Rule rule = new DeviceMetadataResTo.Rule("temp_rule", 0.0, 100.0);

        return new DeviceMetadataResTo(
            location,
            deviceName,
            deviceType,
            "ACTIVE",
            List.of(rule)
        );
    }

    private String createValidJsonResponse(String batchId, Map<UUID, DeviceMetadataResTo> metadata)
            throws Exception {
        MetadataBatchResponse response = new MetadataBatchResponse(batchId, metadata);
        return objectMapper.writeValueAsString(response);
    }

    // ==================== Successful Processing Tests ====================

    @Test
    @DisplayName("Should consume and process valid metadata batch response from Kafka")
    void testConsumeValidResponse() throws Exception {
        // Arrange
        String batchId = "batch-123";
        UUID deviceId1 = UUID.randomUUID();
        UUID deviceId2 = UUID.randomUUID();
        Map<UUID, DeviceMetadataResTo> metadata = new HashMap<>();
        metadata.put(deviceId1, createDeviceMetadata("Device1", "Sensor"));
        metadata.put(deviceId2, createDeviceMetadata("Device2", "Actuator"));

        String message = createValidJsonResponse(batchId, metadata);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message));

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<MetadataBatchResponse> captor = ArgumentCaptor.forClass(MetadataBatchResponse.class);
                verify(enricher, atLeastOnce()).completeEnrichment(captor.capture());

                MetadataBatchResponse response = captor.getValue();
                assertEquals(batchId, response.batchId());
                assertEquals(2, response.metadata().size());
                assertTrue(response.metadata().containsKey(deviceId1));
                assertTrue(response.metadata().containsKey(deviceId2));
            });
    }

    @Test
    @DisplayName("Should consume and process response with single metadata entry")
    void testConsumeSingleMetadata() throws Exception {
        // Arrange
        String batchId = "batch-456";
        UUID deviceId = UUID.randomUUID();
        Map<UUID, DeviceMetadataResTo> metadata = new HashMap<>();
        metadata.put(deviceId, createDeviceMetadata("Device1", "Sensor"));

        String message = createValidJsonResponse(batchId, metadata);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message));

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<MetadataBatchResponse> captor = ArgumentCaptor.forClass(MetadataBatchResponse.class);
                verify(enricher, atLeastOnce()).completeEnrichment(captor.capture());
                
                MetadataBatchResponse response = captor.getValue();
                assertEquals(batchId, response.batchId());
                assertEquals(1, response.metadata().size());
            });
    }

    @Test
    @DisplayName("Should consume and process multiple responses from Kafka")
    void testConsumeMultipleResponses() throws Exception {
        // Arrange
        UUID deviceId1 = UUID.randomUUID();
        UUID deviceId2 = UUID.randomUUID();
        UUID deviceId3 = UUID.randomUUID();
        String message1 = createValidJsonResponse("batch-1", Map.of(deviceId1, createDeviceMetadata("Device1", "Sensor")));
        String message2 = createValidJsonResponse("batch-2", Map.of(deviceId2, createDeviceMetadata("Device2", "Actuator")));
        String message3 = createValidJsonResponse("batch-3", Map.of(deviceId3, createDeviceMetadata("Device3", "Gateway")));

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(
                new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message1),
                new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key2", message2),
                new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key3", message3)
            );

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(enricher, atLeast(3)).completeEnrichment(any());
            });
    }

    @Test
    @DisplayName("Should process response with empty metadata map")
    void testConsumeEmptyMetadata() throws Exception {
        // Arrange
        String batchId = "batch-empty";
        Map<UUID, DeviceMetadataResTo> metadata = new HashMap<>();

        String message = createValidJsonResponse(batchId, metadata);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message));

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<MetadataBatchResponse> captor = ArgumentCaptor.forClass(MetadataBatchResponse.class);
                verify(enricher, atLeastOnce()).completeEnrichment(captor.capture());
                
                MetadataBatchResponse response = captor.getValue();
                assertEquals(batchId, response.batchId());
                assertEquals(0, response.metadata().size());
            });
    }

    // ==================== Invalid Batch ID Tests ====================

    @Test
    @DisplayName("Should skip processing when batch ID is null")
    void testConsumeNullBatchId() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Map<UUID, DeviceMetadataResTo> metadata = new HashMap<>();
        metadata.put(deviceId, createDeviceMetadata("Device1", "Sensor"));

        String message = createValidJsonResponse(null, metadata);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message));

        // Assert - should not call enricher for null batch ID
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                verify(enricher, never()).completeEnrichment(any());
            });
    }

    @Test
    @DisplayName("Should skip processing when batch ID is empty")
    void testConsumeEmptyBatchId() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Map<UUID, DeviceMetadataResTo> metadata = new HashMap<>();
        metadata.put(deviceId, createDeviceMetadata("Device1", "Sensor"));

        String message = createValidJsonResponse("", metadata);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message));

        // Assert - should not call enricher for empty batch ID
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                verify(enricher, never()).completeEnrichment(any());
            });
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void testConsumeInvalidJson() {
        // Arrange
        String invalidMessage = "invalid json";

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", invalidMessage));

        // Assert - should not crash, enricher should not be called
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                verify(enricher, never()).completeEnrichment(any());
            });
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testConsumeMalformedJson() {
        // Arrange
        String malformedMessage = "{\"batchId\": \"batch-123\", \"metadata\": {malformed}}";

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", malformedMessage));

        // Assert - should not crash
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                verify(enricher, never()).completeEnrichment(any());
            });
    }

    @Test
    @DisplayName("Should continue processing after enricher exception")
    void testContinueAfterEnricherException() throws Exception {
        // Arrange
        UUID deviceId1 = UUID.randomUUID();
        UUID deviceId2 = UUID.randomUUID();
        String message1 = createValidJsonResponse("batch-1", Map.of(deviceId1, createDeviceMetadata("Device1", "Sensor")));
        String message2 = createValidJsonResponse("batch-2", Map.of(deviceId2, createDeviceMetadata("Device2", "Actuator")));

        // First call throws exception, second call succeeds
        doThrow(new RuntimeException("Enrichment error"))
            .doNothing()
            .when(enricher).completeEnrichment(any());

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message1));

        // Wait for first message to be processed
        await().pollDelay(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(3))
            .untilAsserted(() -> verify(enricher, atLeastOnce()).completeEnrichment(any()));

        // Send second message
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key2", message2));

        // Assert - should have called enricher at least twice
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                verify(enricher, atLeast(2)).completeEnrichment(any());
            });
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle empty message")
    void testConsumeEmptyMessage() {
        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", ""));

        // Assert - should not crash
        await().pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                verify(enricher, never()).completeEnrichment(any());
            });
    }

    @Test
    @DisplayName("Should handle response with large metadata map")
    void testConsumeLargeMetadata() throws Exception {
        // Arrange
        String batchId = "batch-large";
        Map<UUID, DeviceMetadataResTo> metadata = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            metadata.put(UUID.randomUUID(), createDeviceMetadata("Device" + i, "Sensor"));
        }

        String message = createValidJsonResponse(batchId, metadata);

        // Act
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key1", message));

        // Assert
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ArgumentCaptor<MetadataBatchResponse> captor = ArgumentCaptor.forClass(MetadataBatchResponse.class);
                verify(enricher, atLeastOnce()).completeEnrichment(captor.capture());
                
                MetadataBatchResponse response = captor.getValue();
                assertEquals(batchId, response.batchId());
                assertEquals(100, response.metadata().size());
            });
    }

    @Test
    @DisplayName("Should handle high throughput of responses")
    void testHighThroughput() throws Exception {
        // Arrange - send 30 responses
        for (int i = 0; i < 30; i++) {
            String batchId = "batch-" + i;
            Map<UUID, DeviceMetadataResTo> metadata = Map.of(
                UUID.randomUUID(), createDeviceMetadata("Device" + i, "Sensor")
            );
            String message = createValidJsonResponse(batchId, metadata);
            
            companion.produce(String.class, String.class)
                .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, "key" + i, message));
        }

        // Assert
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                verify(enricher, atLeast(30)).completeEnrichment(any());
            });
    }
}

