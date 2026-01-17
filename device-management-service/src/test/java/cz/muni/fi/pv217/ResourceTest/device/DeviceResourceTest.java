package cz.muni.fi.pv217.ResourceTest.device;

import cz.muni.fi.pv217.devicemanagementservice.domain.DeviceStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class DeviceResourceTest {

    // UUIDs from the SQL Seed Data
    private static final String THERMOSTAT_ID = "1a1b1c1d-2e2f-3a3b-4c4d-5e5f5a5b5c5d";
    private static final String SMART_LIGHT_ID = "b2c3d4e5-f6e5-4d3c-2b1a-0fedcba98765";
    private static final String MOTION_SENSOR_ID = "c3d4e5f6-a1b2-3c4d-5e6f-789012345678";

    // --- Utility Coordinates for Testing (Matching Seed Data Precision) ---
    private static final Double PRAGUE_LONGITUDE = 50.0755;
    private static final Double PRAGUE_LATITUDE = 14.4378;
    private static final Double NEW_LONGITUDE = 18.0123;
    private static final Double NEW_LATITUDE = 48.9876;
    // ----------------------------------------------------------------------

    @Test
    public void testGetAllDevices() {
        given()
                .when()
                .get("/devices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // Verify we have at least the 3 seeded devices
                .body("size()", greaterThanOrEqualTo(2))
                // Verify specific names exist in the list
                .body("name", hasItems("Living Room Thermostat", "Hallway Smart Light"));
    }

    @Test
    public void testGetThermostatById() {
        given()
                .pathParam("id", THERMOSTAT_ID)
                .when()
                .get("/devices/{id}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(THERMOSTAT_ID))
                .body("name", equalTo("Living Room Thermostat"))
                .body("type", equalTo("TEMP_SENSOR"))
                .body("status", equalTo("ACTIVE"))
                // Verify coordinates from seed data (Note: REST Assured often casts Double to Float for comparison)
                .body("longitude", equalTo(PRAGUE_LONGITUDE.floatValue()))
                .body("latitude", equalTo(PRAGUE_LATITUDE.floatValue()));
    }

    @Test
    public void testGetDeviceNotFound() {
        given()
                .pathParam("id", UUID.randomUUID().toString())
                .when()
                .get("/devices/{id}")
                .then()
                .statusCode(404);
    }

    // ------------------------------------ CREATE TESTS ------------------------------------

    @Test
    @TestTransaction
    public void testCreateNewDeviceWithCoordinates() {
        // Test case 4a: Create with BOTH Longitude and Latitude
        var requestBody = Map.of(
                "name", "Kitchen Smoke Detector",
                "type", "SMOKE_SENSOR",
                "status", "ACTIVE",
                "longitude", NEW_LONGITUDE,
                "latitude", NEW_LATITUDE
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/devices")
                .then()
                .statusCode(201)
                .header("Location", containsString("/devices/"))
                .body("name", equalTo("Kitchen Smoke Detector"))
                .body("longitude", equalTo(NEW_LONGITUDE.floatValue()))
                .body("latitude", equalTo(NEW_LATITUDE.floatValue()))
                .body("id", notNullValue());
    }

    @Test
    @TestTransaction
    public void testCreateNewDeviceWithoutCoordinates() {
        // Test case 4b: Create with NEITHER Longitude nor Latitude
        var requestBody = Map.of(
                "name", "Garage Door Sensor",
                "type", "DOOR_SENSOR",
                "status", "ACTIVE"
                // No longitude or latitude fields included
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/devices")
                .then()
                .statusCode(201)
                .header("Location", containsString("/devices/"))
                .body("name", equalTo("Garage Door Sensor"))
                // Verify that the fields are null/absent in the response body
                .body("longitude", nullValue())
                .body("latitude", nullValue())
                .body("id", notNullValue());
    }

    @Test
    @TestTransaction
    public void testCreateDeviceFailsWithOnlyLongitude() {
        // Test case 4c: Fails if only Longitude is provided
        var requestBody = Map.of(
                "name", "Bad Location Device",
                "type", "BAD_SENSOR",
                "status", "ACTIVE",
                "longitude", 10.0 // Missing latitude
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/devices")
                .then()
                .statusCode(400) // Expect Bad Request due to custom validation failure
                .body("violations.message", hasItem(containsString("Longitude and Latitude must be provided together")));
    }

    @Test
    @TestTransaction
    public void testCreateDeviceFailsWithOnlyLatitude() {
        // Test case 4d: Fails if only Latitude is provided
        var requestBody = Map.of(
                "name", "Another Bad Device",
                "type", "BAD_SENSOR",
                "status", "ACTIVE",
                "latitude", 10.0 // Missing longitude
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/devices")
                .then()
                .statusCode(400) // Expect Bad Request due to custom validation failure
                .body("violations.message", hasItem(containsString("Longitude and Latitude must be provided together")));
    }

    // ------------------------------------ OTHER TESTS ------------------------------------

    @Test
    @TestTransaction
    public void testCreateDeviceInvalidStatus() {
        var requestBody = Map.of(
                "name", "Test Invalid Status",
                "type", "THERMOSTAT",
                "status", "INVALID_STATUS" // Invalid status value
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/devices")
                .then()
                .statusCode(400); // Expect Bad Request
    }


    @Test
    @TestTransaction // Run after creation tests
    public void testCreateDeviceMissingFields() {
        // Missing 'name' and 'type' fields
        var requestBody = Map.of(
                "status", "ACTIVE",
                "longitude", 0.0,
                "latitude", 0.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/devices")
                .then()
                .statusCode(400) // Expect Bad Request
                .body("title", equalTo("Constraint Violation"))
                .body("violations.field", hasItems("create.request.name", "create.request.type"));
    }

    // ------------------------------------ UPDATE TESTS ------------------------------------

    @Test
    @TestTransaction
    public void testUpdateSmartLightWithNewCoordinates() {
        // Updating the existing Smart Light from Seed with new coordinates
        var updateBody = Map.of(
                "id", SMART_LIGHT_ID,
                "name", "Hallway Smart Light",
                "type", "LIGHT",
                "status", DeviceStatus.INACTIVE.name(), // Changing status to INACTIVE
                "longitude", NEW_LONGITUDE,
                "latitude", NEW_LATITUDE,
                "description", "updated description"
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/devices")
                .then()
                .statusCode(200)
                .body("id", equalTo(SMART_LIGHT_ID))
                .body("status", equalTo(DeviceStatus.INACTIVE.name()))
                .body("longitude", equalTo(NEW_LONGITUDE.floatValue()))
                .body("latitude", equalTo(NEW_LATITUDE.floatValue()));
    }

    @Test
    @TestTransaction
    public void testUpdateFailsWithMismatchedCoordinates() {
        Map<String, Object> updateBody = new HashMap<>();

        updateBody.put("id", THERMOSTAT_ID);
        updateBody.put("name", "Living Room Thermostat");
        updateBody.put("type", "TEMP_SENSOR");
        updateBody.put("status", DeviceStatus.ACTIVE.name());
        updateBody.put("longitude", 10.0); // Present
        updateBody.put("latitude", null);   // Allowed in HashMap
        updateBody.put("description", "description");

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/devices")
                .then()
                .statusCode(400) // Expect Bad Request due to custom validation failure
                .body("violations.message", hasItem(containsString("Longitude and Latitude must be provided together")));
    }


    @Test
    @TestTransaction
    public void testUpdateDeviceNotFound() {
        String randomId = UUID.randomUUID().toString();
        var updateBody = Map.of(
                "id", randomId,
                "name", "Ghost Device",
                "type", "UNKNOWN",
                "status", "ACTIVE",
                "longitude", 0.0,
                "latitude", 0.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/devices")
                .then()
                .statusCode(404);
    }

    @Test
    @TestTransaction
    public void testUpdateDeviceInvalidId() {
        var updateBody = Map.of(
                "id", "giberish-not-uuid",
                "name", "A Name",
                "type", "A Type",
                "status", DeviceStatus.ACTIVE.name(),
                "longitude", 0.0,
                "latitude", 0.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/devices")
                .then()
                .statusCode(400); // Expect Bad Request
    }

    @Test
    @TestTransaction
    public void testUpdateDeviceBlankName() {
        var updateBody = Map.of(
                "id", THERMOSTAT_ID,
                "name", "",
                "type", "A Type",
                "status", DeviceStatus.ACTIVE.name(),
                "longitude", 0.0,
                "latitude", 0.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/devices")
                .then()
                .statusCode(400); // Expect Bad Request
    }

    @Test
    @TestTransaction
    public void testUpdateDeviceBlankType() {
        var updateBody = Map.of(
                "id", THERMOSTAT_ID,
                "name", "A Name",
                "type", "",
                "status", DeviceStatus.ACTIVE.name(),
                "longitude", 0.0,
                "latitude", 0.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/devices")
                .then()
                .statusCode(400); // Expect Bad Request
    }


    // ------------------------------------ DELETE TESTS ------------------------------------

    @Test
    @TestTransaction
    public void testDeleteMotionSensor() {
        // Deleting the seed data Motion Sensor
        given()
                .pathParam("id", MOTION_SENSOR_ID)
                .when()
                .delete("/devices/{id}")
                .then()
                .statusCode(204);

        // Verify it is actually gone
        given()
                .pathParam("id", MOTION_SENSOR_ID)
                .when()
                .get("/devices/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @TestTransaction
    public void testDeleteNotFound() {
        given()
                .pathParam("id", UUID.randomUUID().toString())
                .when()
                .delete("/devices/{id}")
                .then()
                .statusCode(404);
    }
}