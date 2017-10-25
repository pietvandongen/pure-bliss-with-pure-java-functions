package com.pietvandongen.purebliss.offlinedetector.service;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;

import java.time.Instant;
import java.util.Optional;

public interface PushNotificationService {

    /**
     * Sends an offline notification for a device.
     *
     * @param device The device to send the notification for.
     */
    void sendOfflineNotification(Device device);

    /**
     * Gets the last offline notification for the given device, or nothing if not present.
     *
     * @param device The device to get the last offline notification for.
     * @return The last offline notification, or empty if is not present.
     */
    Optional<Instant> getLastOfflineNotificationInstant(Device device);
}
