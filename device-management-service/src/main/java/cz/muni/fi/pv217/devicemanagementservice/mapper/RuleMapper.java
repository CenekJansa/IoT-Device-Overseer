package cz.muni.fi.pv217.devicemanagementservice.mapper;

import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.domain.Rule;
import cz.muni.fi.pv217.devicemanagementservice.dto.rule.CreateRuleRequest;
import cz.muni.fi.pv217.devicemanagementservice.dto.rule.UpdateRuleRequest;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.DeviceNotFoundException;
import cz.muni.fi.pv217.devicemanagementservice.service.DeviceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
@ApplicationScoped
public final class RuleMapper {

    @Inject
    DeviceService service;

    public Rule mapCreateRequestToRule(CreateRuleRequest request, Rule rule) {

        rule.ruleName = request.ruleName();
        rule.fromValue = request.fromValue();
        rule.toValue = request.toValue();

        try {
            rule.device = service.findDeviceById(request.deviceId());
        } catch (DeviceNotFoundException e) {
            // add log
            throw e;
        }

        if (request.description() != null) {
            rule.description = request.description();
        }

        return rule;
    }

    public Rule mapUpdateRequestToRule(UpdateRuleRequest request, Rule rule) {
        rule.id = request.id();

        if (request.ruleName() != null) {
            rule.ruleName = request.ruleName();
        }

        if (request.fromValue() != null) {
            rule.fromValue = request.fromValue();
        }

        if (request.toValue() != null) {
            rule.toValue = request.toValue();
        }

        try {
            if (request.deviceId() != null) {
                rule.device = service.findDeviceById(request.deviceId());
            }

        } catch (DeviceNotFoundException e) {
            // add log
            throw e;
        }
        if (request.description() != null) {
            rule.description = request.description();
        }

        return rule;
    }
}
