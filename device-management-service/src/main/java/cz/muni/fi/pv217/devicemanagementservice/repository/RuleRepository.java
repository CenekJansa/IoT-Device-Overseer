package cz.muni.fi.pv217.devicemanagementservice.repository;

import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.domain.DeviceStatus;
import cz.muni.fi.pv217.devicemanagementservice.domain.Rule;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
@ApplicationScoped
public class RuleRepository implements PanacheRepositoryBase<Rule, UUID> {
    public List<Rule> findRulesByDeviceId(UUID deviceId) {
        return list("device.id = ?1",  deviceId);
    }
}

