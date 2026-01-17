package cz.muni.fi.pv217.devicemanagementservice.api;

import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.domain.Rule;
import cz.muni.fi.pv217.devicemanagementservice.dto.rule.CreateRuleRequest;
import cz.muni.fi.pv217.devicemanagementservice.dto.rule.UpdateRuleRequest;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.DeviceNotFoundException;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.RuleNotFoundException;
import cz.muni.fi.pv217.devicemanagementservice.service.DeviceService;
import cz.muni.fi.pv217.devicemanagementservice.service.RuleService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Path("/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RuleResource {

    @Inject
    RuleService service;

    @Inject
    DeviceService deviceService;

    // --- C: Create (POST /rules) ---
    @POST
    @Timed("create_rule_processing_time")
    @Counted("create_rule_request_count")
    public Response create(@Valid CreateRuleRequest request) {
        try {
            var rule = service.createRule(request);
            return Response.created(URI.create("/rules/" + rule.id)).entity(rule).build();
        } catch (DeviceNotFoundException e) {
            Map<String, String> errorResponse = Map.of(
                    "error", "Device Not Found for rule",
                    "message", e.getMessage() // Use the exception's message for details
            );
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Timed("get_rule_processing_time")
    @Counted("get_rule_request_count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        var rules = service.findAllRules();
        return Response.ok(rules).build();
    }

    @GET
    @Path("/{id}")
    @Timed("get_rule_by_id_processing_time")
    @Counted("get_rule_by_id_request_count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") UUID id) {
        try {
            Rule rule = service.findRuleById(id);
            return Response.ok(rule).build();
        } catch (RuleNotFoundException e) {
            // LOG.error("Rule not found: {}, {}", id,  e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/device/{device_id}")
    @Timed("get_rule_by_device_id_processing_time")
    @Counted("get_rule_by_device_id_request_count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleByDeviceId(@PathParam("device_id") UUID deviceId) {
        try {
            deviceService.findDeviceById(deviceId);
        } catch (DeviceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No device with ID: " + deviceId + " does not exists")
                    .build();
        }

        return Response.ok(service.findRuleByDeviceId(deviceId)).build();
    }

    @PUT
    @Timed("update_rule_processing_time")
    @Counted("update_rule_request_count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@Valid UpdateRuleRequest request) {
        try {
            var updatedRule = service.updateRule(request.id(), request);
            return Response.ok(updatedRule).build();
        } catch (RuleNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (DeviceNotFoundException e) {
            Map<String, String> errorResponse = Map.of(
                    "error", "Device Not Found for rule",
                    "message", e.getMessage() // Use the exception's message for details
            );
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @DELETE
    @Timed("delete_rule_processing_time")
    @Counted("delete_rule_request_count")
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        if (service.deleteRule(id)) {
            // Returns 204 No Content for a successful deletion
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
