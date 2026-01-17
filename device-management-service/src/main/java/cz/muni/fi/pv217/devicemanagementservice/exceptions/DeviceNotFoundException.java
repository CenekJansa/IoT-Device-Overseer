package cz.muni.fi.pv217.devicemanagementservice.exceptions;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String message) { super(message); }
}
