package cz.muni.fi.pv217.devicemanagementservice.service;

import cz.muni.fi.pv217.devicemanagementservice.domain.Device;
import cz.muni.fi.pv217.devicemanagementservice.dto.device.CreateDeviceRequest;
import cz.muni.fi.pv217.devicemanagementservice.dto.device.UpdateDeviceRequest;
import cz.muni.fi.pv217.devicemanagementservice.exceptions.DeviceNotFoundException;
import cz.muni.fi.pv217.devicemanagementservice.mapper.DeviceMapper;
import cz.muni.fi.pv217.devicemanagementservice.repository.DeviceRepository;
import jakarta.enterprise.context.ApplicationScoped; // <-- Correct Scope for Services
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DeviceService {

    @Inject
    DeviceRepository repository;

    @Transactional
    public Device createDevice(CreateDeviceRequest request) {
        Device device = DeviceMapper.mapCreateRequestToDevice(request, new Device());
        repository.persist(device);

        return device;
    }

    // --- R: Read ---
    public Device findDeviceById(UUID id) {
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device with id: '" + id + "' not found"));
    }

    public List<Device> findAllDevices() {
        return repository.listAll();
    }


    @Transactional
    public Device updateDevice(UUID id, UpdateDeviceRequest request) {
        Device existingDevice = repository.findByIdOptional(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device with id: '" + id + "' not found"));

        return DeviceMapper.mapUpdateRequestToDevice(request, existingDevice);
    }

    @Transactional
    public boolean deleteDevice(UUID id) {
        return repository.deleteById(id);
    }

}