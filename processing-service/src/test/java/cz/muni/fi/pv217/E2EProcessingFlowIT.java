package cz.muni.fi.pv217;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.entity.to.DeviceMetadataResTo;
import cz.muni.fi.pv217.entity.to.ProcessedEventTo;
import cz.muni.fi.pv217.kafka.dto.MetadataBatchRequest;
import cz.muni.fi.pv217.kafka.dto.MetadataBatchResponse;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(KafkaAndRedisTestResource.class)
@DisplayName("End-to-End Processing Flow Integration Test")
class E2EProcessingFlowIT {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    ObjectMapper objectMapper;

    private static final String RAW_TELEMETRY_TOPIC = "sensor-ingest";
    private static final String METADATA_BATCH_REQUESTS_TOPIC = "metadata-batch-requests";
    private static final String METADATA_BATCH_RESPONSES_TOPIC = "metadata-batch-responses";
    private static final String PROCESSED_DATA_TOPIC = "processing-data";

    @Test
    @DisplayName("Should process sensor data end-to-end: ingest -> metadata request -> metadata response -> processed output")
    void testEndToEndProcessingFlow() throws Exception {
        // ==================== SETUP ====================

        // Define test UUIDs and timestamps
        UUID deviceId100 = UUID.fromString("00000000-0000-0000-0000-000000000100");
        UUID deviceId200 = UUID.fromString("00000000-0000-0000-0000-000000000200");
        UUID deviceId300 = UUID.fromString("00000000-0000-0000-0000-000000000300");
        Instant timestamp1 = Instant.ofEpochMilli(1000L);
        Instant timestamp2 = Instant.ofEpochMilli(2000L);
        Instant timestamp3 = Instant.ofEpochMilli(3000L);

        // Storage for captured metadata requests
        List<MetadataBatchRequest> capturedRequests = new CopyOnWriteArrayList<>();

        // Storage for processed events
        List<ProcessedEventTo> processedEvents = new CopyOnWriteArrayList<>();

        // Start consuming from metadata-batch-requests topic to simulate device management service
        ConsumerTask<String, String> metadataRequestConsumer = companion.consume(String.class, String.class)
            .fromTopics(METADATA_BATCH_REQUESTS_TOPIC, 10);

        // Start consuming from processed data topic to validate final output
        ConsumerTask<String, String> processedDataConsumer = companion.consume(String.class, String.class)
            .fromTopics(PROCESSED_DATA_TOPIC, 10);

        // ==================== STEP 1: Send sensor data to sensor-ingest topic ====================

        String sensorData1 = createSensorDataJson(deviceId100, timestamp1, List.of(
            Map.of("temperature", 25.5, "humidity", 60.0)
        ));

        String sensorData2 = createSensorDataJson(deviceId200, timestamp2, List.of(
            Map.of("temperature", 35.0, "pressure", 1013.25)
        ));

        String sensorData3 = createSensorDataJson(deviceId300, timestamp3, List.of(
            Map.of("temperature", 15.0, "humidity", 55.0)
        ));

        companion.produce(String.class, String.class)
            .fromRecords(
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key1", sensorData1),
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key2", sensorData2),
                new ProducerRecord<>(RAW_TELEMETRY_TOPIC, "key3", sensorData3)
            );
        
        // ==================== STEP 2: Wait for and validate metadata batch request ====================
        
        AtomicReference<String> batchIdRef = new AtomicReference<>();
        
        await().atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                List<ConsumerRecord<String, String>> records = metadataRequestConsumer.getRecords();
                assertFalse(records.isEmpty(), "Should have received metadata batch request");
                
                // Parse the metadata request
                ConsumerRecord<String, String> record = records.get(0);
                MetadataBatchRequest request = objectMapper.readValue(record.value(), MetadataBatchRequest.class);
                
                // Validate the request
                assertNotNull(request.batchId(), "Batch ID should not be null");
                assertNotNull(request.deviceIds(), "Device IDs should not be null");
                assertEquals(3, request.deviceIds().size(), "Should request metadata for 3 devices");
                assertTrue(request.deviceIds().contains(deviceId100), "Should include device 100");
                assertTrue(request.deviceIds().contains(deviceId200), "Should include device 200");
                assertTrue(request.deviceIds().contains(deviceId300), "Should include device 300");
                
                capturedRequests.add(request);
                batchIdRef.set(request.batchId());
            });
        
        String batchId = batchIdRef.get();
        assertNotNull(batchId, "Batch ID should be captured");
        
        // ==================== STEP 3: Mock device management service response ====================
        
        // Create metadata for each device
        Map<UUID, DeviceMetadataResTo> metadataMap = new HashMap<>();

        // Device 100: temperature rule 15-30, humidity rule 40-70
        metadataMap.put(deviceId100, new DeviceMetadataResTo(
            Map.of("latitude", 50.0, "longitude", 14.0),
            "Temperature Sensor 100",
            "TemperatureSensor",
            "ACTIVE",
            List.of(
                new DeviceMetadataResTo.Rule("temperature", 15.0, 30.0),
                new DeviceMetadataResTo.Rule("humidity", 40.0, 70.0)
            )
        ));
        
        // Device 200: temperature rule 15-30 (will violate with 35.0), pressure rule 900-1100
        metadataMap.put(deviceId200, new DeviceMetadataResTo(
            Map.of("latitude", 51.0, "longitude", 15.0),
            "Multi Sensor 200",
            "MultiSensor",
            "ACTIVE",
            List.of(
                new DeviceMetadataResTo.Rule("temperature", 15.0, 30.0),
                new DeviceMetadataResTo.Rule("pressure", 900.0, 1100.0)
            )
        ));
        
        // Device 300: temperature rule 10-25 (will be OK with 15.0), humidity rule 50-80
        metadataMap.put(deviceId300, new DeviceMetadataResTo(
            Map.of("latitude", 52.0, "longitude", 16.0),
            "Humidity Sensor 300",
            "HumiditySensor",
            "ACTIVE",
            List.of(
                new DeviceMetadataResTo.Rule("temperature", 10.0, 25.0),
                new DeviceMetadataResTo.Rule("humidity", 50.0, 80.0)
            )
        ));
        
        // Send metadata response
        MetadataBatchResponse response = new MetadataBatchResponse(batchId, metadataMap);
        String responseJson = objectMapper.writeValueAsString(response);
        
        companion.produce(String.class, String.class)
            .fromRecords(new ProducerRecord<>(METADATA_BATCH_RESPONSES_TOPIC, batchId, responseJson));
        
        // ==================== STEP 4: Wait for and validate processed data output ====================
        
        await().atMost(Duration.ofSeconds(20))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                List<ConsumerRecord<String, String>> records = processedDataConsumer.getRecords();
                assertFalse(records.isEmpty(), "Should have received processed data");
                assertTrue(records.size() >= 3, "Should have at least 3 processed events");
                
                // Parse all processed events
                processedEvents.clear();
                for (ConsumerRecord<String, String> record : records) {
                    ProcessedEventTo event = objectMapper.readValue(record.value(), ProcessedEventTo.class);
                    processedEvents.add(event);
                }
                
                // Validate we have events for all 3 devices
                assertTrue(processedEvents.stream().anyMatch(e -> e.getDeviceId().equals(deviceId100)),
                    "Should have processed event for device 100");
                assertTrue(processedEvents.stream().anyMatch(e -> e.getDeviceId().equals(deviceId200)),
                    "Should have processed event for device 200");
                assertTrue(processedEvents.stream().anyMatch(e -> e.getDeviceId().equals(deviceId300)),
                    "Should have processed event for device 300");
            });
        
        // ==================== STEP 5: Detailed validation of processed events ====================
        
        // Validate Device 100 (temperature 25.5 is OK, humidity 60.0 is OK)
        ProcessedEventTo event100 = processedEvents.stream()
            .filter(e -> e.getDeviceId().equals(deviceId100))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Device 100 event not found"));

        assertEquals(timestamp1, event100.getTimestamp(), "Device 100 timestamp should match");
        assertNotNull(event100.getMetrics(), "Device 100 should have metrics");
        assertEquals(2, event100.getMetrics().size(), "Device 100 should have 2 metrics");
        
        ProcessedEventTo.ProcessedEventMetricTo tempMetric100 = event100.getMetrics().stream()
            .filter(m -> m.getMetricName().equals("temperature"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Temperature metric not found for device 100"));
        assertEquals(25.5, tempMetric100.getValue(), 0.01, "Temperature value should be 25.5");
        assertFalse(tempMetric100.isViolatingSafety(), "Temperature 25.5 should NOT violate safety (15-30)");
        
        ProcessedEventTo.ProcessedEventMetricTo humidityMetric100 = event100.getMetrics().stream()
            .filter(m -> m.getMetricName().equals("humidity"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Humidity metric not found for device 100"));
        assertEquals(60.0, humidityMetric100.getValue(), 0.01, "Humidity value should be 60.0");
        assertFalse(humidityMetric100.isViolatingSafety(), "Humidity 60.0 should NOT violate safety (40-70)");
        
        // Validate Device 200 (temperature 35.0 VIOLATES, pressure 1013.25 is OK)
        ProcessedEventTo event200 = processedEvents.stream()
            .filter(e -> e.getDeviceId().equals(deviceId200))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Device 200 event not found"));

        assertEquals(timestamp2, event200.getTimestamp(), "Device 200 timestamp should match");
        assertNotNull(event200.getMetrics(), "Device 200 should have metrics");
        assertEquals(2, event200.getMetrics().size(), "Device 200 should have 2 metrics");
        
        ProcessedEventTo.ProcessedEventMetricTo tempMetric200 = event200.getMetrics().stream()
            .filter(m -> m.getMetricName().equals("temperature"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Temperature metric not found for device 200"));
        assertEquals(35.0, tempMetric200.getValue(), 0.01, "Temperature value should be 35.0");
        assertTrue(tempMetric200.isViolatingSafety(), "Temperature 35.0 SHOULD violate safety (15-30)");
        
        ProcessedEventTo.ProcessedEventMetricTo pressureMetric200 = event200.getMetrics().stream()
            .filter(m -> m.getMetricName().equals("pressure"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Pressure metric not found for device 200"));
        assertEquals(1013.25, pressureMetric200.getValue(), 0.01, "Pressure value should be 1013.25");
        assertFalse(pressureMetric200.isViolatingSafety(), "Pressure 1013.25 should NOT violate safety (900-1100)");
        
        // Validate Device 300 (temperature 15.0 is OK, humidity 55.0 is OK)
        ProcessedEventTo event300 = processedEvents.stream()
            .filter(e -> e.getDeviceId().equals(deviceId300))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Device 300 event not found"));

        assertEquals(timestamp3, event300.getTimestamp(), "Device 300 timestamp should match");
        assertNotNull(event300.getMetrics(), "Device 300 should have metrics");
        assertEquals(2, event300.getMetrics().size(), "Device 300 should have 2 metrics");
        
        ProcessedEventTo.ProcessedEventMetricTo tempMetric300 = event300.getMetrics().stream()
            .filter(m -> m.getMetricName().equals("temperature"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Temperature metric not found for device 300"));
        assertEquals(15.0, tempMetric300.getValue(), 0.01, "Temperature value should be 15.0");
        assertFalse(tempMetric300.isViolatingSafety(), "Temperature 15.0 should NOT violate safety (10-25)");
        
        ProcessedEventTo.ProcessedEventMetricTo humidityMetric300 = event300.getMetrics().stream()
            .filter(m -> m.getMetricName().equals("humidity"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Humidity metric not found for device 300"));
        assertEquals(55.0, humidityMetric300.getValue(), 0.01, "Humidity value should be 55.0");
        assertFalse(humidityMetric300.isViolatingSafety(), "Humidity 55.0 should NOT violate safety (50-80)");

        // ==================== STEP 5: Validate Device Metadata Fields ====================

        // Validate Device 100 metadata
        assertNotNull(event100.getDeviceName(), "Device 100 should have device name");
        assertEquals("Device 100", event100.getDeviceName(), "Device 100 name should match");
        assertNotNull(event100.getDeviceType(), "Device 100 should have device type");
        assertEquals("Sensor", event100.getDeviceType(), "Device 100 type should be Sensor");
        assertNotNull(event100.getDeviceStatus(), "Device 100 should have device status");
        assertEquals("ACTIVE", event100.getDeviceStatus(), "Device 100 status should be ACTIVE");
        assertNotNull(event100.getLocation(), "Device 100 should have location");
        assertNotNull(event100.getRules(), "Device 100 should have rules");

        // Validate Device 200 metadata
        assertNotNull(event200.getDeviceName(), "Device 200 should have device name");
        assertEquals("Device 200", event200.getDeviceName(), "Device 200 name should match");
        assertNotNull(event200.getDeviceType(), "Device 200 should have device type");
        assertEquals("Sensor", event200.getDeviceType(), "Device 200 type should be Sensor");
        assertNotNull(event200.getDeviceStatus(), "Device 200 should have device status");
        assertEquals("ACTIVE", event200.getDeviceStatus(), "Device 200 status should be ACTIVE");
        assertNotNull(event200.getLocation(), "Device 200 should have location");
        assertNotNull(event200.getRules(), "Device 200 should have rules");

        // Validate Device 300 metadata
        assertNotNull(event300.getDeviceName(), "Device 300 should have device name");
        assertEquals("Device 300", event300.getDeviceName(), "Device 300 name should match");
        assertNotNull(event300.getDeviceType(), "Device 300 should have device type");
        assertEquals("Sensor", event300.getDeviceType(), "Device 300 type should be Sensor");
        assertNotNull(event300.getDeviceStatus(), "Device 300 should have device status");
        assertEquals("ACTIVE", event300.getDeviceStatus(), "Device 300 status should be ACTIVE");
        assertNotNull(event300.getLocation(), "Device 300 should have location");
        assertNotNull(event300.getRules(), "Device 300 should have rules");
    }

    // ==================== Helper Methods ====================

    private String createSensorDataJson(UUID deviceId, Instant timestamp, List<Map<String, Double>> readings) throws Exception {
        Map<String, Object> message = new HashMap<>();
        message.put("deviceId", deviceId.toString());
        message.put("timestamp", timestamp.toString());
        message.put("readings", readings);
        return objectMapper.writeValueAsString(message);
    }
}

