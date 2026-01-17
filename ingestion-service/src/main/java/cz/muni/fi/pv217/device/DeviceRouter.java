package cz.muni.fi.pv217.device;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ingest")
public class DeviceRouter {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRouter.class);

    @Inject
    MeterRegistry registry;

    @Inject
    @Channel("sensor-ingest")
    Emitter<DevicePayload> sensorEmitter;

    @Inject
    @RestClient
    ManagementServiceClient deviceClient;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Timed("ingest_request_processing_time")
    @Counted("ingest_request_count")
    public String ingest(@RequestBody DevicePayload data) {
        try {
            // for some reason is the 404 treated as exception..
            Response resp = deviceClient.getDeviceByUuid(String.valueOf(data.getDeviceId()));
            if (resp.getStatus() != 200) {
                LOG.error("Failed to find device with id `{}` in device-manager.", data.getDeviceId());
                return "nok";
            }
        } catch (Exception e) {
            LOG.error("Error finding the device configured `{}`", e.getMessage());
            return "nok";
        }
        sensorEmitter.send(data);
        return "ok";
    }
}
