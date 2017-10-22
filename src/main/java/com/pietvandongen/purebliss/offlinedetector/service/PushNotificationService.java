package com.pietvandongen.purebliss.offlinedetector.service;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;

import java.time.Instant;
import java.util.Optional;

public interface PushNotificationService {

    void sendOfflineNotification(Device device);

    Optional<Instant> getLastOfflineNotificationInstant(Device device);
}
