package cz.muni.fi.pv217.ResourceTest.ping;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class PingResourceTest {
    @Test
    void testPingEndpoint() {
        given()
                .when().get("/devices/ping")
                .then()
                .statusCode(200)
                .body(is("pong"));
    }

}