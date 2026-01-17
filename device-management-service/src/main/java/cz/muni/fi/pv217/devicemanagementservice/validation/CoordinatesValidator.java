package cz.muni.fi.pv217.devicemanagementservice.validation;

// CoordinatesValidator.java

import cz.muni.fi.pv217.devicemanagementservice.dto.device.UpdateDeviceRequest;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.MissingCoordinateException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import cz.muni.fi.pv217.devicemanagementservice.dto.device.CreateDeviceRequest;
// or UpdateDeviceRequest, depending on which class you apply it to

public class CoordinatesValidator implements ConstraintValidator<CoordinatesRequired, Object> {

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        final Double longitude;
        final Double latitude;

        if (object instanceof CreateDeviceRequest request) {
            longitude = request.longitude();
            latitude = request.latitude();
        } else if (object instanceof UpdateDeviceRequest request) {
            longitude = request.longitude();
            latitude = request.latitude();
        } else {
            throw new MissingCoordinateException("Longitude and Latitude must be provided together or both omitted.");
        }

        boolean bothNull = (longitude == null && latitude == null);
        boolean bothPresent = (longitude != null && latitude != null);

        return bothNull || bothPresent;
    }
}