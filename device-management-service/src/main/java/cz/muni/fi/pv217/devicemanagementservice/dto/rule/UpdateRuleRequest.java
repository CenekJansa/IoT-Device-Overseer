package cz.muni.fi.pv217.devicemanagementservice.dto.rule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateRuleRequest(

        @NotNull(message = "ID cannot be null")
        UUID id,

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String ruleName,

        Integer fromValue,

        Integer toValue,

        String description,

        UUID deviceId
) {
}
