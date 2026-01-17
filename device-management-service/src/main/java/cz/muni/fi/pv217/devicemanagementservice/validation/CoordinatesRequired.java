package cz.muni.fi.pv217.devicemanagementservice.validation;

// CoordinatesRequired.java

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CoordinatesValidator.class)
@Documented
public @interface CoordinatesRequired {
    String message() default "Longitude and Latitude must be provided together or both omitted.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}