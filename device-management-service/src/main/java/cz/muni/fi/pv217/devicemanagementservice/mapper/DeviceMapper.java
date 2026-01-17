package cz.muni.fi.pv217.devicemanagementservice.mapper;

import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.dto.device.CreateDeviceRequest;
import cz.muni.fi.pv217.devicemanagementservice.dto.device.UpdateDeviceRequest;

public final class DeviceMapper {

    private DeviceMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Device mapCreateRequestToDevice(CreateDeviceRequest request, Device device) {
        device.name = request.name();
        device.type = request.type();
        device.status = request.status();

        if (request.description() != null) {
            device.description = request.description();
        }

        if (request.latitude() != null) {
            device.latitude = request.latitude();
        }

        if (request.longitude() != null) {
            device.longitude = request.longitude();
        }

        return device;
    }

    public static Device mapUpdateRequestToDevice(UpdateDeviceRequest request, Device device) {
        device.id = request.id();

        if (request.name() != null ) {
            device.name = request.name();
        }

        if (request.type() != null) {
            device.type = request.type().toUpperCase();
        }

        if (request.status() != null) {
            device.status = request.status();
        }

        if (request.description() != null) {
            device.description = request.description();
        }

        if (request.latitude() != null) {
            device.latitude = request.latitude();
        }

        if (request.longitude() != null) {
            device.longitude = request.longitude();
        }

        return device;
    }
}
