package com.pietvandongen.purebliss.offlinedetector.service;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;

import java.time.Instant;
import java.util.Map;

public interface DeviceService {

    /**
     * Gets a map of offline device, with the devices as keys and the instant they went offline as values.
     *
     * @return The map of offline devices.
     */
    Map<Device, Instant> getOfflineDevices();
}
