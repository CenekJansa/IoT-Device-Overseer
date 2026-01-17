package utils;

import cz.muni.fi.pv217.device.ManagementServiceClient;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Alternative
@Priority(1) // ensure it overrides the real client
public class ManagementServiceClientMock implements ManagementServiceClient {

    @Override
    public Response getDeviceByUuid(String uuid) {
        // You can simulate a successful response with status 200
        // Optionally, you can also include a dummy JSON payload
        String dummyJson = "{ \"uuid\": \"" + uuid + "\", \"name\": \"Test Device\" }";

        return Response
                .status(Response.Status.OK) // HTTP 200
                .entity(dummyJson)          // JSON payload
                .build();
    }
}
