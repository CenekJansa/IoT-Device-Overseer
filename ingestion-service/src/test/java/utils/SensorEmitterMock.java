package utils;

import cz.muni.fi.pv217.device.DevicePayload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Alternative
@Priority(1)
public class SensorEmitterMock {
    public static List<DevicePayload> sentMessages = new ArrayList<>();

    public void send(DevicePayload payload) {
        sentMessages.add(payload);
    }

}
