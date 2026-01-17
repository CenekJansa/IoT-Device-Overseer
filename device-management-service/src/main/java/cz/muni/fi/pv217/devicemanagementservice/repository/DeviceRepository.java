package cz.muni.fi.pv217.devicemanagementservice.repository;

import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.domain.DeviceStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DeviceRepository implements PanacheRepositoryBase<Device, UUID> {

    // Custom query example
    public List<Device> findActiveDevicesByLocation(String location) {
        return list("status = ?1 and location = ?2", DeviceStatus.ACTIVE, location);
    }
}