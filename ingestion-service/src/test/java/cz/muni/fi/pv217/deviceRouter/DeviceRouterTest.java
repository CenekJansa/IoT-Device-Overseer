package cz.muni.fi.pv217.deviceRouter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cz.muni.fi.pv217.device.DevicePayload;
import cz.muni.fi.pv217.device.ManagementServiceClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@DisplayName("DeviceRouter Integration Tests")
class DeviceRouterTest {

    @Inject
    @Connector("smallrye-in-memory")
    InMemoryConnector connector;

    @InjectMock
    @RestClient
    ManagementServiceClient managementServiceClient;

    @BeforeEach
    void setUp() {
        // Clear the in-memory connector before each test
        connector.sink("sensor-ingest").clear();
        Mockito.reset(managementServiceClient);
    }

    @Test
    @DisplayName("Should successfully ingest device payload when device exists")
    void testIngestEndpoint_Success() {
        // Given: A valid device ID and payload
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Instant now = Instant.now();

        String json = """
            {
              "deviceId": "%s",
              "timestamp": "%s",
              "readings": [
                {
                  "temperature": 22.5,
                  "humidity": 55
                }
              ]
            }
            """.formatted(deviceId, now.toString());

        // Mock the device management service to return a successful response
        Response mockResponse = Response.status(Response.Status.OK)
            .entity("{\"id\": \"" + deviceId + "\", \"name\": \"Test Device\"}")
            .build();
        when(managementServiceClient.getDeviceByUuid(deviceId.toString())).thenReturn(mockResponse);

        // When: Calling the /ingest endpoint
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .when()
            .post("/ingest")
            .then()
            .statusCode(200)
            .body(is("ok"));

        // Then: Verify the device management service was called
        verify(managementServiceClient).getDeviceByUuid(deviceId.toString());

        // And: Verify the message was sent to Kafka topic
        var sink = connector.sink("sensor-ingest");
        assertEquals(1, sink.received().size(), "Expected 1 message in the Kafka sink");

        // And: Verify the message content
        var messages = sink.received();
        Object payloadObj = messages.get(0).getPayload();
        assertTrue(payloadObj instanceof DevicePayload,
            "Payload should be a DevicePayload instance");

        DevicePayload payload = (DevicePayload) payloadObj;
        assertNotNull(payload, "Payload should not be null");
        assertEquals(deviceId, payload.getDeviceId(), "Device ID should match");
        assertEquals(now, payload.getTimestamp(), "Timestamp should match");
        assertNotNull(payload.getReadings(), "Readings should not be null");
        assertEquals(1, payload.getReadings().size(), "Should have 1 reading");

        Map<String, Object> reading = payload.getReadings().get(0);
        assertEquals(22.5, reading.get("temperature"), "Temperature should match");
        assertEquals(55, reading.get("humidity"), "Humidity should match");
    }

    @Test
    @DisplayName("Should return 'nok' when device does not exist")
    void testIngestEndpoint_DeviceNotFound() {
        // Given: A device ID that doesn't exist
        UUID deviceId = UUID.randomUUID();
        Instant now = Instant.now();

        String json = """
            {
              "deviceId": "%s",
              "timestamp": "%s",
              "readings": [
                {
                  "temperature": 22.5,
                  "humidity": 55
                }
              ]
            }
            """.formatted(deviceId, now.toString());

        // Mock the device management service to return a 404 Not Found response
        Response mockResponse = Response.status(Response.Status.NOT_FOUND).build();
        when(managementServiceClient.getDeviceByUuid(deviceId.toString())).thenReturn(mockResponse);

        // When: Calling the /ingest endpoint
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .when()
            .post("/ingest")
            .then()
            .statusCode(200)
            .body(is("nok"));

        // Then: Verify the device management service was called
        verify(managementServiceClient).getDeviceByUuid(deviceId.toString());

        // And: Verify NO message was sent to Kafka topic
        var sink = connector.sink("sensor-ingest");
        assertEquals(0, sink.received().size(),
            "Expected 0 messages in the Kafka sink when device not found");
    }

    @Test
    @DisplayName("Should return 'nok' when device management service returns error")
    void testIngestEndpoint_ServiceError() {
        // Given: A valid device ID but service returns error
        UUID deviceId = UUID.randomUUID();
        Instant now = Instant.now();

        String json = """
            {
              "deviceId": "%s",
              "timestamp": "%s",
              "readings": [
                {
                  "temperature": 22.5,
                  "humidity": 55
                }
              ]
            }
            """.formatted(deviceId, now.toString());

        // Mock the device management service to return a 500 Internal Server Error
        Response mockResponse = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        when(managementServiceClient.getDeviceByUuid(deviceId.toString())).thenReturn(mockResponse);

        // When: Calling the /ingest endpoint
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .when()
            .post("/ingest")
            .then()
            .statusCode(200)
            .body(is("nok"));

        // Then: Verify the device management service was called
        verify(managementServiceClient).getDeviceByUuid(deviceId.toString());

        // And: Verify NO message was sent to Kafka topic
        var sink = connector.sink("sensor-ingest");
        assertEquals(0, sink.received().size(),
            "Expected 0 messages in the Kafka sink when service error occurs");
    }

    @Test
    @DisplayName("Should handle multiple readings in a single payload")
    void testIngestEndpoint_MultipleReadings() {
        // Given: A payload with multiple readings
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Instant now = Instant.now();

        String json = """
            {
              "deviceId": "%s",
              "timestamp": "%s",
              "readings": [
                {
                  "temperature": 22.5,
                  "humidity": 55
                },
                {
                  "temperature": 23.0,
                  "humidity": 56
                }
              ]
            }
            """.formatted(deviceId, now.toString());

        // Mock the device management service to return a successful response
        Response mockResponse = Response.status(Response.Status.OK)
            .entity("{\"id\": \"" + deviceId + "\", \"name\": \"Test Device\"}")
            .build();
        when(managementServiceClient.getDeviceByUuid(deviceId.toString())).thenReturn(mockResponse);

        // When: Calling the /ingest endpoint
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .when()
            .post("/ingest")
            .then()
            .statusCode(200)
            .body(is("ok"));

        // Then: Verify the message was sent to Kafka topic
        var sink = connector.sink("sensor-ingest");
        assertEquals(1, sink.received().size(), "Expected 1 message in the Kafka sink");

        // And: Verify the message contains multiple readings
        var messages = sink.received();
        Object payloadObj = messages.get(0).getPayload();
        assertTrue(payloadObj instanceof DevicePayload,
            "Payload should be a DevicePayload instance");

        DevicePayload payload = (DevicePayload) payloadObj;
        assertNotNull(payload.getReadings(), "Readings should not be null");
        assertEquals(2, payload.getReadings().size(), "Should have 2 readings");
    }
}
