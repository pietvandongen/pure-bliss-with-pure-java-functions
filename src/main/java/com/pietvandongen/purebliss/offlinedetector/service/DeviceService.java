package com.pietvandongen.purebliss.offlinedetector.service;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;

import java.time.Instant;
import java.util.Map;

public interface DeviceService {

    Map<Device, Instant> getOfflineDevices();
}
