package cz.muni.fi.pv217.devicemanagementservice.dto.device;

import cz.muni.fi.pv217.devicemanagementservice.domain.DeviceStatus;
import cz.muni.fi.pv217.devicemanagementservice.validation.CoordinatesRequired;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@CoordinatesRequired
public record UpdateDeviceRequest(
        @NotNull(message = "ID cannot be null")
        UUID id,

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Type is required")
        @Size(max = 50)
        String type, // e.g., TEMP_SENSOR

        DeviceStatus status, // e.g., ACTIVE

        Double longitude,

        Double latitude,

        @Size(max = 512)
        String description
) {}

