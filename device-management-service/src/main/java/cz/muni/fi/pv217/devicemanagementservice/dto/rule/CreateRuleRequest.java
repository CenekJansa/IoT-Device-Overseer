package cz.muni.fi.pv217.devicemanagementservice.dto.rule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRuleRequest (

    @NotNull
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    String ruleName,

    @NotNull
    Integer fromValue,

    @NotNull
    Integer toValue,

    String description,

    @NotNull(message = "Device ID is required")
    UUID deviceId

    )
{}
