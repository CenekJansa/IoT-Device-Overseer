package cz.muni.fi.pv217.ResourceTest.rule;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RuleResourceTest {

    private static final String THERMOSTAT_ID = "1a1b1c1d-2e2f-3a3b-4c4d-5e5f5a5b5c5d";
    private static final String SMART_LIGHT_ID = "b2c3d4e5-f6e5-4d3c-2b1a-0fedcba98765";
    private static final String MOTION_SENSOR_ID = "c3d4e5f6-a1b2-3c4d-5e6f-789012345678";
    // UUIDs from the SQL Seed Data (Hypothetical for Rule Seed Data)
    private static final String TEMP_RULE_ID = "89d5c071-79aa-4def-9272-1b3b1aecbb1e";
    private static final String LIGHT_RULE_ID = "dfaa62df-1b0c-4ed7-8421-1fc7cf45879f";
    private static final String MOTION_RULE_ID = "2a8f897f-8f07-4a1f-8bc8-70ad3b08edaa";


    // ------------------------------------ READ TESTS ------------------------------------

    @Test
    @Order(1)
    public void testGetAllRules() {
        given()
                .when()
                .get("/rules")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // Verify we have at least the 3 seeded rules
                .body("size()", greaterThanOrEqualTo(2))
                // Verify specific names exist in the list
                .body("ruleName", hasItems("temperature", "brightness"));
    }

    @Test
    @Order(2)
    public void testGetRuleById() {
        given()
                .pathParam("id", TEMP_RULE_ID)
                .when()
                .get("/rules/{id}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(TEMP_RULE_ID))
                .body("ruleName", equalTo("temperature"))
                .body("fromValue", equalTo(18))
                .body("description", equalTo("Low Temp Auto-Adjust: If the temperature falls below 18 degrees, set the target temperature to 22 degrees."))
                .body("device.id", equalTo(THERMOSTAT_ID));
    }

    @Test
    @Order(3)
    public void testGetRuleNotFound() {
        given()
                .pathParam("id", UUID.randomUUID().toString())
                .when()
                .get("/rules/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    public void testGetRulesByDeviceIdThermostat() {
        given()
                .pathParam("device_id", THERMOSTAT_ID)
                .when()
                .get("/rules/device/{device_id}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("[0].ruleName", equalTo("temperature"));
    }

    @Test
    @Order(5)
    public void testGetRulesByDeviceIdSmartLight() {
        given()
                .pathParam("device_id", SMART_LIGHT_ID)
                .when()
                .get("/rules/device/{device_id}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(greaterThan(0))) // Ensure a list is returned with at least one item
                .body("[0].ruleName", equalTo("brightness"))
                .body("[0].fromValue", equalTo(30)); // Check a specific value for robustness
    }

    @Test
    @Order(6)
    public void testGetRulesByDeviceIdDeletedMotionSensor() {
        given()
                .pathParam("device_id", MOTION_SENSOR_ID)
                .when()
                .get("/rules/device/{device_id}")
                .then()
                .statusCode(404);
    }
    @Test
    @Order(61)
    public void testGetRulesByDeviceIdRandomId() {
        given()
                .pathParam("device_id", UUID.randomUUID())
                .when()
                .get("/rules/device/{device_id}")
                .then()
                .statusCode(404);
    }

    // ------------------------------------ CREATE TESTS ------------------------------------

    @Test
    @Order(7)
    public void testCreateNewRule() {
        var requestBody = Map.of(
                "ruleName", "New Test Rule",
                "fromValue", 23,
                "toValue", 488,
                "description", "new description",
                "deviceId", THERMOSTAT_ID
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/rules")
                .then()
                .statusCode(201)
                .body("ruleName", containsString("New Test Rule"))
                .body("fromValue", equalTo(23))
                .body("toValue", equalTo(488))
                .body("description", equalTo("new description"))
                .body("device.id", equalTo(THERMOSTAT_ID));
    }

    @Test
    @Order(8)
    public void testCreateRuleMissingRequiredFields() {
        // Missing 'name' and 'action' fields
        var requestBody = Map.of(
                "ruleName", "Cooling rule"
                // 'enabled' defaults to false or is allowed to be missing
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/rules")
                .then()
                .statusCode(400) // Expect Bad Request due to constraint violations
                .body("title", equalTo("Constraint Violation"))
                .body("violations.field", hasItems("create.request.fromValue", "create.request.toValue", "create.request.deviceId"));
    }

    // --- Custom Validation Test (Assuming a Rule must have BOTH a Trigger and an Action) ---
    @Test
    @Order(9)
    public void testCreateRuleFailsWithoutToValue() {
        var requestBody = Map.of(
                "ruleName", "Rule Missing Action",
                "fromValue", 20,
                "enabled", true
                // Missing 'toValue'
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/rules")
                .then()
                .statusCode(400) // Expect Bad Request from standard validation
                .body("title", equalTo("Constraint Violation"))
                .body("violations.field", hasItem("create.request.toValue"));
    }

    @Test
    @Order(9)
    public void testCreateRuleInvalidTypeForFromValue() {
        var requestBody = Map.of(
                "ruleName", "Bad Type Rule",
                "fromValue", "not-a-number", // Invalid type
                "toValue", 100,
                "deviceId", THERMOSTAT_ID
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/rules")
                .then()
                .statusCode(400);

    }

    @Test
    @Order(10)
    public void testCreateRuleRuleNameTooLong() {
        String longName = "A".repeat(256); // 256 characters long
        var requestBody = Map.of(
                "ruleName", longName,
                "fromValue", 1,
                "toValue", 2,
                "deviceId", THERMOSTAT_ID
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/rules")
                .then()
                .statusCode(400)
                .body("title", equalTo("Constraint Violation"))
                .body("violations.field", hasItem("create.request.ruleName"));
    }

    // ------------------------------------ UPDATE TESTS ------------------------------------

    @Test
    @Order(11)
    public void testUpdateRuleActionAndStatus() {
        // Update the existing Light Rule
        var updateBody = Map.of(
                "id", LIGHT_RULE_ID,
                "ruleName", "Updated Night Light Rule",
                "fromValue", -1200,
                "toValue", -800,
                "deviceId", THERMOSTAT_ID,
                "description", "Updated description"

        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/rules")
                .then()
                .statusCode(200)
                .body("id", equalTo(LIGHT_RULE_ID))
                .body("ruleName", equalTo("Updated Night Light Rule"))
                .body("fromValue", equalTo(-1200))
                .body("toValue", equalTo(-800))
                .body("description", equalTo("Updated description"));
    }

    @Test
    @Order(12)
    public void testUpdateRuleNotFound() {
        String randomId = UUID.randomUUID().toString();
        var updateBody = Map.of(
                "id", randomId,
                "ruleName", "Non-Existent Rule",
                "description", "New description for Non-existent Rule",
                "fromValue", 49,
                "toValue", 95,
                "deviceId", THERMOSTAT_ID

        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/rules")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(13)
    public void testUpdateRuleInvalidId() {
        var updateBody = Map.of(
                "id", "not-a-valid-uuid",
                "name", "A Name",
                "triggerCondition", "trigger",
                "action", "action",
                "enabled", true
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/rules")
                .then()
                .statusCode(400); // Expect Bad Request due to ID format validation
    }

    @Test
    @Order(14)
    public void testUpdateRulePartialUpdate() {
        // 1. Get the original description to assert later
        String originalDescription = given()
                .pathParam("id", LIGHT_RULE_ID)
                .when()
                .get("/rules/{id}")
                .then()
                .extract().path("description");

        // 2. Update ONLY the ruleName and required fields (omitting description)
        var partialUpdateBody = Map.of(
                "id", LIGHT_RULE_ID,
                "ruleName", "Partial Update Test",
                "fromValue", 10,
                "toValue", 50,
                "deviceId", THERMOSTAT_ID
                // Description is intentionally missing
        );

        given()
                .contentType(ContentType.JSON)
                .body(partialUpdateBody)
                .when()
                .put("/rules")
                .then()
                .statusCode(200)
                .body("ruleName", equalTo("Partial Update Test"))
                // Assert that the original description was retained
                .body("description", equalTo(originalDescription));
    }

    @Test
    @Order(15)
    public void testUpdateRuleWithNonExistingDeviceId() {
        String nonExistentDeviceId = UUID.randomUUID().toString();

        var updateBody = Map.of(
                "id", LIGHT_RULE_ID,
                "ruleName", "Failing Device Link",
                "fromValue", 10,
                "toValue", 20,
                "deviceId", nonExistentDeviceId // Device does not exist
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/rules")
                .then()
                // Should be 404 NOT_FOUND or 400 BAD_REQUEST depending on your error handling
                .statusCode(404);
    }

    // ------------------------------------ DELETE TESTS ------------------------------------

    @Test
    @Order(16)
    public void testDeleteMotionRule() {
        // Deleting the seed data Motion Rule
        given()
                .pathParam("id", TEMP_RULE_ID)
                .when()
                .delete("/rules/{id}")
                .then()
                .statusCode(204);

        // Verify it is actually gone
        given()
                .pathParam("id", TEMP_RULE_ID)
                .when()
                .get("/rules/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(17)
    public void testDeleteNotFound() {
        given()
                .pathParam("id", UUID.randomUUID().toString())
                .when()
                .delete("/rules/{id}")
                .then()
                .statusCode(404);
    }


}