package com.pietvandongen.purebliss.offlinedetector.job.documented;

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

/**
 * Runs a job periodically that sends out device offline push notifications if a device is offline for a certain
 * amount of time. The thresholds that determine whether the notification should be pushed are configurable before
 * and at runtime.
 */
public class OfflineDevicesJobImpl implements OfflineDevicesJob {

    private final DeviceService deviceService;
    private final PushNotificationService pushNotificationService;
    private final Map<Device, Instant> offlineDevices;

    private List<Duration> thresholds = new ArrayList<>();

    /**
     * Injects the job's dependencies and uses them to configure its initial state.
     *
     * @param deviceService           The device service.
     * @param pushNotificationService The push notification service.
     */
    OfflineDevicesJobImpl(DeviceService deviceService, PushNotificationService pushNotificationService) {
        if (deviceService == null || pushNotificationService == null) {
            throw new IllegalArgumentException("Device service nor push notification service can be null.");
        }

        this.deviceService = deviceService;
        this.pushNotificationService = pushNotificationService;
        this.offlineDevices = new ConcurrentHashMap<>(deviceService.getOfflineDevices());
    }

    /**
     * Calculates the last passed threshold given a start point and a current point in time plus a list of thresholds.
     * If the amount of time passed between the start and current instant is less than the first interval, it returns
     * empty.
     * If not, the amount is checked against each fixed interval and the calculated intervals after it. As soon
     * as the last passed interval has been determined, it will be returned.
     *
     * @param start      The start instant to compare the current instant with.
     * @param current    The current instant to compare with the starting point.
     * @param thresholds The list of fixed push notification thresholds.
     * @return The last passed threshold, or empty if no threshold has been passed yet.
     */
    static Optional<Duration> calculateLastPassedThreshold(Instant start, Instant current, List<Duration> thresholds) {
        if (current.isBefore(start) || start.equals(current) || thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("Start must be before current and there should be at least 1 threshold");
        }

        Duration timePassed = Duration.between(start, current);

        if (timePassed.compareTo(thresholds.get(0)) <= 0) {
            return Optional.empty();
        }

        for (int i = 1; i < thresholds.size(); i++) {
            if (timePassed.compareTo(thresholds.get(i)) <= 0) {
                return Optional.of(thresholds.get(i - 1));
            }
        }

        return Optional.of(thresholds.get(thresholds.size() - 1));
    }

    /**
     * Checks whether a notification should be sent by determining which threshold has been passed last for the
     * calculated amount of time passed between the device going offline and the job running, taking the last sent
     * notification info account.
     *
     * @param jobStart         The instant the job calling this function was started.
     * @param deviceOffline    The instant the device went offline.
     * @param lastNotification The instant the last notification was sent.
     * @param thresholds       The list of notification thresholds.
     * @return True if the notification should be sent, false if not.
     */
    static boolean shouldSendNotification(Instant jobStart, Instant deviceOffline, Instant lastNotification, List<Duration> thresholds) {
        Optional<Duration> lastPassedThreshold = calculateLastPassedThreshold(deviceOffline, jobStart, thresholds);

        return lastPassedThreshold.isPresent() && (lastNotification.isBefore(deviceOffline) || !lastPassedThreshold.equals(calculateLastPassedThreshold(deviceOffline, lastNotification, thresholds)));
    }

    /**
     * Same as {@link OfflineDevicesJobImpl#shouldSendNotification(Instant, Instant, List)}, but doesn't take any
     * previously sent push notification into account.
     *
     * @see OfflineDevicesJobImpl#shouldSendNotification(Instant, Instant, List)
     */
    static boolean shouldSendNotification(Instant jobStart, Instant deviceOffline, List<Duration> thresholds) {
        return calculateLastPassedThreshold(deviceOffline, jobStart, thresholds).isPresent();
    }

    /**
     * A predicate to determine if a notification should be sent at the given instant.
     *
     * @param jobStart The instant the job calling this function was started.
     * @return The predicate, which returns true if a notification should be sent, false if not.
     */
    private Predicate<Map.Entry<Device, Instant>> shouldSendNotificationAfter(Instant jobStart) {
        return offlineDevice -> pushNotificationService.getLastOfflineNotificationInstant(offlineDevice.getKey())
                .map(notification -> shouldSendNotification(jobStart, offlineDevice.getValue(), notification, thresholds))
                .orElseGet(() -> shouldSendNotification(jobStart, offlineDevice.getValue(), thresholds));
    }

    /**
     * Sends push notifications for offline devices, once per passed threshold.
     */
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

    /**
     * Configures the thresholds for the job. There should be at least one threshold.
     *
     * @param thresholds The threshold to be used by the job.
     */
    private void setThresholds(List<Duration> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("There should be at least 1 threshold.");
        }

        this.thresholds = thresholds;
    }
}
