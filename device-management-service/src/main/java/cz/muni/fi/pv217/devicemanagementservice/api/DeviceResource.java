package cz.muni.fi.pv217.devicemanagementservice.api;


import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.dto.device.CreateDeviceRequest;
import cz.muni.fi.pv217.devicemanagementservice.dto.device.UpdateDeviceRequest;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.DeviceNotFoundException;
import cz.muni.fi.pv217.devicemanagementservice.service.DeviceService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource {

    @Inject
    DeviceService service;

    // --- C: Create (POST /devices) ---
    @POST
    @Timed("create_device_processing_time")
    @Counted("create_device_request_count")
    public Response create(@Valid CreateDeviceRequest request) {
        var device = service.createDevice(request);

        // Returns 201 Created and the location of the new resource
        return Response.created(URI.create("/devices/" + device.id)).entity(device).build();
    }

    @GET
    @Timed("getall_device_processing_time")
    @Counted("getall_device_request_count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        var devices = service.findAllDevices();
        return Response.ok(devices).build();
    }

    @GET
    @Path("/{id}")
    @Timed("get_by_id_processing_time")
    @Counted("get_by_id_request_count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") UUID id) {
        try {
            Device device = service.findDeviceById(id);
            return Response.ok(device).build();
        } catch (DeviceNotFoundException e) {
            // LOG.error("Device not found: {}, {}", id,  e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Timed("update_device_processing_time")
    @Counted("update_device_request_count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@Valid UpdateDeviceRequest request) {
        try {
            var updatedDevice = service.updateDevice(request.id(), request);
            return Response.ok(updatedDevice).build();
        } catch (DeviceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Timed("delete_device_processing_time")
    @Counted("delete_device_request_count")
    public Response delete(@PathParam("id") UUID id) {
        if (service.deleteDevice(id)) {
            // Returns 204 No Content for a successful deletion
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}