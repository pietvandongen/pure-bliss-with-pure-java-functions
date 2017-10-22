package com.pietvandongen.purebliss.offlinedetector.job.round1;

import com.pietvandongen.purebliss.offlinedetector.domain.Device;
import com.pietvandongen.purebliss.offlinedetector.job.OfflineDevicesJob;
import com.pietvandongen.purebliss.offlinedetector.service.DeviceService;
import com.pietvandongen.purebliss.offlinedetector.service.PushNotificationService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class OfflineDevicesJobImpl implements OfflineDevicesJob {

    private final Clock clock;
    private final DeviceService deviceService;
    private final PushNotificationService pushNotificationService;
    private final Map<Device, Instant> offlineDevices;

    private List<Duration> thresholds = new ArrayList<>();

    OfflineDevicesJobImpl(
            Clock clock,
            DeviceService deviceService,
            PushNotificationService pushNotificationService
    ) {
        if (clock == null || deviceService == null || pushNotificationService == null) {
            throw new IllegalArgumentException("Clock, device service not push notification service can be null.");
        }

        this.clock = clock;
        this.deviceService = deviceService;
        this.pushNotificationService = pushNotificationService;
        this.offlineDevices = new ConcurrentHashMap<>(deviceService.getOfflineDevices());
    }

    @Override
    public void run() {
        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalStateException("Thresholds cannot be null or empty.");
        }

        Instant now = Instant.now(clock);

        for (Map.Entry<Device, Instant> offlineDevice : offlineDevices.entrySet()) {
            Device device = offlineDevice.getKey();
            Instant disconnectInstant = offlineDevice.getValue();
            Optional<Instant> lastOfflineNotificationInstant = pushNotificationService.getLastOfflineNotificationInstant(device);
            Duration firstThreshold = thresholds.get(0);
            Duration timePassedSinceDeviceWentOffline = Duration.between(disconnectInstant, now);
            boolean firstThresholdWasPassed = timePassedSinceDeviceWentOffline.compareTo(firstThreshold) > 0;

            if (firstThresholdWasPassed) {
                if (thresholds.size() == 1) {
                    if (!lastOfflineNotificationInstant.isPresent()) {
                        pushNotificationService.sendOfflineNotification(device);
                        break;
                    } else {
                        Duration timePassedBetweenDisconnectAndLastNotification = Duration.between(disconnectInstant, lastOfflineNotificationInstant.get());

                        if (timePassedBetweenDisconnectAndLastNotification.isNegative()) {
                            pushNotificationService.sendOfflineNotification(device);
                            break;
                        } else {
                            break;
                        }
                    }
                } else {
                    Duration lastThreshold = thresholds.get(thresholds.size() - 1);

                    for (int i = 1; i <= thresholds.size(); i++) {
                        Duration previousThreshold = thresholds.get(i - 1);

                        if (i == thresholds.size()) {
                            if (!lastOfflineNotificationInstant.isPresent()) {
                                pushNotificationService.sendOfflineNotification(device);
                                break;
                            } else {
                                Duration timePassedBetweenDisconnectAndLastNotification = Duration.between(disconnectInstant, lastOfflineNotificationInstant.get());

                                if (!timePassedBetweenDisconnectAndLastNotification.isNegative()
                                        && timePassedBetweenDisconnectAndLastNotification.compareTo(lastThreshold) <= 0) {
                                    pushNotificationService.sendOfflineNotification(device);
                                    break;
                                }
                            }
                        } else {
                            Duration currentThreshold = thresholds.get(i);

                            if (timePassedSinceDeviceWentOffline.compareTo(previousThreshold) > 0
                                    && timePassedSinceDeviceWentOffline.compareTo(currentThreshold) <= 0) {
                                if (!lastOfflineNotificationInstant.isPresent()) {
                                    pushNotificationService.sendOfflineNotification(device);
                                    break;
                                } else {
                                    Duration timePassedBetweenDisconnectAndLastNotification = Duration.between(disconnectInstant, lastOfflineNotificationInstant.get());

                                    if (!timePassedBetweenDisconnectAndLastNotification.isNegative()
                                            && timePassedBetweenDisconnectAndLastNotification.compareTo(previousThreshold) > 0) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDeviceConnect(Device device) {
        this.offlineDevices.remove(device);
    }

    @Override
    public void onDeviceDisconnect(Device device) {
        this.offlineDevices.put(device, Instant.now(clock));
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
