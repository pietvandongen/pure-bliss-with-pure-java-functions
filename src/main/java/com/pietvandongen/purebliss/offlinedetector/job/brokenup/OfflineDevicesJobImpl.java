package com.pietvandongen.purebliss.offlinedetector.job.brokenup;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;
import com.pietvandongen.purebliss.offlinedetector.job.OfflineDevicesJob;
import com.pietvandongen.purebliss.offlinedetector.service.DeviceService;
import com.pietvandongen.purebliss.offlinedetector.service.PushNotificationService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class OfflineDevicesJobImpl implements OfflineDevicesJob {

    private final DeviceService deviceService;
    private final PushNotificationService pushNotificationService;
    private final Map<Device, Instant> offlineDevices;

    private List<Duration> thresholds = new ArrayList<>();

    OfflineDevicesJobImpl(DeviceService deviceService, PushNotificationService pushNotificationService) {
        if (deviceService == null || pushNotificationService == null) {
            throw new IllegalArgumentException("Device service nor push notification service can be null.");
        }

        this.deviceService = deviceService;
        this.pushNotificationService = pushNotificationService;
        this.offlineDevices = new ConcurrentHashMap<>(deviceService.getOfflineDevices());
    }

    static Optional<Duration> calculateLastPassedThreshold(Instant start, Instant current, List<Duration> thresholds) {
        if (current.isBefore(start) || start.equals(current) || thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("Start must be before current and there should be at least 1 threshold");
        }

        Duration timePassed = Duration.between(start, current);

        if (timePassed.compareTo(thresholds.get(0)) <= 0) {
            return Optional.empty();
        }

        for (int i = 0; i < thresholds.size(); i++) {
            if (timePassed.compareTo(thresholds.get(i)) <= 0) {
                return Optional.of(thresholds.get(i - 1));
            }
        }

        return Optional.of(thresholds.get(thresholds.size() - 1));
    }

    static boolean shouldSendNotification(Instant jobStart, Instant deviceOffline, List<Duration> thresholds) {
        return calculateLastPassedThreshold(deviceOffline, jobStart, thresholds).isPresent();
    }

    static boolean shouldSendNotification(Instant jobStart, Instant deviceOffline, Instant lastNotification, List<Duration> thresholds) {
        Optional<Duration> lastPassedThreshold = calculateLastPassedThreshold(deviceOffline, jobStart, thresholds);

        return lastPassedThreshold.isPresent()
                && (lastNotification.isBefore(deviceOffline)
                || !lastPassedThreshold.equals(calculateLastPassedThreshold(deviceOffline, lastNotification, thresholds)));
    }

    public void run() {
        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalStateException("Thresholds cannot be null or empty.");
        }

        Instant jobStart = Instant.now();

        offlineDevices.entrySet().stream()
                .filter(shouldSendNotificationAfter(jobStart))
                .map(Map.Entry::getKey)
                .forEach(pushNotificationService::sendOfflineNotification);
    }

    private Predicate<Map.Entry<Device, Instant>> shouldSendNotificationAfter(Instant jobStart) {
        return offlineDeviceEntry -> shouldSendNotification(jobStart, offlineDeviceEntry.getKey(), offlineDeviceEntry.getValue());
    }

    private boolean shouldSendNotification(Instant jobStart, Device device, Instant deviceOffline) {
        return pushNotificationService.getLastOfflineNotificationInstant(device)
                .map(notification -> shouldSendNotification(jobStart, deviceOffline, notification, thresholds))
                .orElseGet(() -> shouldSendNotification(jobStart, deviceOffline, thresholds));
    }

    @Override
    public void onDeviceConnect(Device device) {
        this.offlineDevices.remove(device);
    }

    @Override
    public void onDeviceDisconnect(Device device) {
        this.offlineDevices.put(device, Instant.now());
    }

    @Override
    public void onConfigurationUpdate(List<Duration> thresholds) {
        setThresholds(thresholds);
    }

    private void setThresholds(List<Duration> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("There should be at least 1 threshold.");
        }

        this.thresholds = thresholds;
    }
}
